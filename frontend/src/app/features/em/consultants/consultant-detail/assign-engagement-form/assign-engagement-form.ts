import { Component, OnInit, effect, inject, input, model, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AssignmentService } from '../../../../../services/assignment.service';
import { EngagementService } from '../../../../../services/engagement.service';
import { Assignment, AssignmentStatus, EngagementRole } from '../../../../../types/assignment.types';
import { Consultant } from '../../../../../types/consultant.types';
import { Engagement, EngagementStatus } from '../../../../../types/engagement.types';

export interface AssignedEngagementRef {
  engagementId: number;
}

@Component({
  selector: 'app-assign-engagement-form',
  imports: [ReactiveFormsModule],
  templateUrl: './assign-engagement-form.html',
  styleUrl: './assign-engagement-form.css',
})
export class AssignEngagementForm implements OnInit {
  private readonly engagementService = inject(EngagementService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly formBuilder = inject(FormBuilder);

  consultant = input<Consultant | null>(null);
  existingAssignments = input<AssignedEngagementRef[]>([]);
  visible = model<boolean>(false);

  assigned = output<Assignment>();

  readonly engagementRoles = Object.values(EngagementRole);
  readonly assignmentStatuses = Object.values(AssignmentStatus);
  readonly engagements = signal<Engagement[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly availableEngagements = signal<Engagement[]>([]);

  form!: FormGroup;

  constructor() {
    this.form = this.formBuilder.nonNullable.group({
      engagementId: [null as number | null, Validators.required],
      engagementRole: [EngagementRole.ASSOCIATE, Validators.required],
      assignmentStartDate: [new Date().toISOString().slice(0, 10), Validators.required],
      assignmentEndDate: ['', Validators.required],
      status: [AssignmentStatus.ACTIVE, Validators.required],
    });

    effect(() => {
      const assignedIds = new Set(this.existingAssignments().map((a) => a.engagementId));
      this.availableEngagements.set(this.engagements().filter((e) => !assignedIds.has(e.id)));
    });

    effect(() => {
      if (this.visible()) {
        this.errorMessage.set(null);
        this.form.reset({
          engagementId: null,
          engagementRole: EngagementRole.ASSOCIATE,
          assignmentStartDate: new Date().toISOString().slice(0, 10),
          assignmentEndDate: '',
          status: AssignmentStatus.ACTIVE,
        });
      }
    });
  }

  private static readonly UNASSIGNABLE_STATUSES = new Set<EngagementStatus>([
    EngagementStatus.COMPLETED,
    EngagementStatus.CANCELLED,
  ]);

  ngOnInit(): void {
    this.engagementService.getAll().subscribe({
      next: (engagements) =>
        this.engagements.set(
          engagements.filter((e) => !AssignEngagementForm.UNASSIGNABLE_STATUSES.has(e.status)),
        ),
      error: () => this.errorMessage.set('Failed to load engagements.'),
    });
  }

  onSubmit(): void {
    const consultant = this.consultant();
    if (!consultant || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);

    const value = this.form.getRawValue();
    this.assignmentService
      .create({
        consultantId: consultant.id,
        engagementId: value.engagementId!,
        engagementRole: value.engagementRole,
        assignmentStartDate: value.assignmentStartDate,
        assignmentEndDate: value.assignmentEndDate,
        status: value.status,
      })
      .subscribe({
        next: (assignment) => {
          this.submitting.set(false);
          this.visible.set(false);
          this.assigned.emit(assignment);
        },
        error: () => {
          this.submitting.set(false);
          this.errorMessage.set('Failed to assign engagement.');
        },
      });
  }

  onCancel(): void {
    this.visible.set(false);
  }
}
