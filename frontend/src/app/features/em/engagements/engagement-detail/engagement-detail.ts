import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EngagementCard } from '../engagement.model';
import { EditDatesModal } from '../editors/edit-dates-modal/edit-dates-modal';
import { EditableBadge } from '../editors/editable-badge/editable-badge';
import { EditableTitle } from '../editors/editable-title/editable-title';
import { EditableSummary } from '../editors/editable-summary/editable-summary';
import { Assignment, EngagementRole } from '../../../../types/assignment.types';
import { Consultant } from '../../../../types/consultant.types';
import { CreateEngagementRequest, EngagementStatus, EngagementType } from '../../../../types/engagement.types';
import { AssignmentService } from '../../../../services/assignment.service';
import { ConsultantService } from '../../../../services/consultant.service';
import { initialsOf, colorOf } from '../../../../shared/avatar';

interface RoleGroup {
  role: EngagementRole;
  label: string;
  assignments: Assignment[];
}

const ROLE_LABELS: Record<EngagementRole, string> = {
  [EngagementRole.LEAD]: 'Lead',
  [EngagementRole.SENIOR_ASSOCIATE]: 'Senior Associate',
  [EngagementRole.ASSOCIATE]: 'Associate',
};

@Component({
  selector: 'app-engagement-detail',
  imports: [FormsModule, EditDatesModal, EditableBadge, EditableTitle, EditableSummary],
  templateUrl: './engagement-detail.html',
  styleUrl: './engagement-detail.css',
})
export class EngagementDetail implements OnInit {
  private readonly assignmentService = inject(AssignmentService);
  private readonly consultantService = inject(ConsultantService);

  @Input() engagement: EngagementCard | null = null;
  @Input() initialStatus: EngagementStatus = EngagementStatus.PLANNED;
  @Output() close = new EventEmitter<void>();
  @Output() create = new EventEmitter<CreateEngagementRequest>();
  @Output() updateSummary = new EventEmitter<string>();
  @Output() updateName = new EventEmitter<string>();
  @Output() updateDates = new EventEmitter<{ startDate: string; targetEndDate: string }>();
  @Output() updateType = new EventEmitter<EngagementType>();
  @Output() updateStatus = new EventEmitter<EngagementStatus>();
  @Output() assigned = new EventEmitter<void>();

  protected readonly engagementTypes = Object.values(EngagementType);
  protected readonly statuses = Object.values(EngagementStatus);
  protected readonly engagementRoles = Object.values(EngagementRole);

  protected readonly expandedRoles = signal<Set<EngagementRole>>(new Set());
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly allConsultants = signal<Consultant[]>([]);

  protected readonly roleGroups = computed<RoleGroup[]>(() => {
    const groups = new Map<EngagementRole, Assignment[]>();

    for (const assignment of this.assignments()) {
      const group = groups.get(assignment.engagementRole);
      if (group) {
        group.push(assignment);
      } else {
        groups.set(assignment.engagementRole, [assignment]);
      }
    }

    return Array.from(groups, ([role, assignments]) => ({
      role,
      label: ROLE_LABELS[role],
      assignments,
    }));
  });

  protected form: CreateEngagementRequest = this.emptyForm();

  protected editingDates = false;

  protected addingConsultant = false;
  protected newConsultantId: number | null = null;
  protected newConsultantRole: EngagementRole = EngagementRole.ASSOCIATE;

  protected get isCreateMode(): boolean {
    return this.engagement === null;
  }

  protected get availableConsultants(): Consultant[] {
    const assignedIds = new Set(this.assignments().map((a) => a.consultantId));
    return this.allConsultants().filter((c) => !assignedIds.has(c.id));
  }

  ngOnInit(): void {
    if (!this.engagement) {
      this.form = this.emptyForm();
      return;
    }

    this.loadConsultantsPanel();
  }

  protected submit(): void {
    this.create.emit(this.form);
  }

  protected startEditDates(): void {
    if (!this.engagement) {
      return;
    }
    this.editingDates = true;
  }

  protected saveDates(dates: { startDate: string; targetEndDate: string }): void {
    this.editingDates = false;
    this.updateDates.emit(dates);
  }

  protected cancelEditDates(): void {
    this.editingDates = false;
  }

  protected onTypeChange(value: string): void {
    this.updateType.emit(value as EngagementType);
  }

  protected onStatusChange(value: string): void {
    this.updateStatus.emit(value as EngagementStatus);
  }

  protected startAddConsultant(): void {
    this.newConsultantId = null;
    this.newConsultantRole = EngagementRole.ASSOCIATE;
    this.addingConsultant = true;
  }

  protected cancelAddConsultant(): void {
    this.addingConsultant = false;
  }

  protected submitAddConsultant(): void {
    if (!this.engagement || this.newConsultantId === null) {
      return;
    }

    this.assignmentService
      .create({
        consultantId: this.newConsultantId,
        engagementId: this.engagement.id,
        engagementRole: this.newConsultantRole,
        assignmentStartDate: new Date().toISOString().slice(0, 10),
      })
      .subscribe({
        next: () => {
          this.addingConsultant = false;
          this.loadConsultantsPanel();
          this.assigned.emit();
        },
        error: (err) => console.error('Failed to assign consultant', err),
      });
  }

  protected initials(name: string): string {
    return initialsOf(name);
  }

  protected colorFor(name: string): string {
    return colorOf(name);
  }

  private loadConsultantsPanel(): void {
    if (!this.engagement) {
      return;
    }
    const engagementId = this.engagement.id;

    this.assignmentService.getByEngagement(engagementId).subscribe({
      next: (assignments) => {
        this.assignments.set(assignments);
        this.expandedRoles.set(new Set(assignments.map((a) => a.engagementRole)));
      },
      error: (err) => console.error(`Failed to load assignments for engagement ${engagementId}`, err),
    });

    this.consultantService.getAll().subscribe({
      next: (consultants) => this.allConsultants.set(consultants),
      error: (err) => console.error('Failed to load consultants', err),
    });
  }

  private emptyForm(): CreateEngagementRequest {
    return {
      engagementName: '',
      clientId: 0,
      engagementType: EngagementType.AUDIT,
      summary: '',
      startDate: '',
      targetEndDate: '',
      status: this.initialStatus,
    };
  }

  protected formatDate(value: string): string {
    const [year, month, day] = value.split('-').map(Number);
    return `${month}/${day}/${year}`;
  }

  protected isExpanded(role: EngagementRole): boolean {
    return this.expandedRoles().has(role);
  }

  protected toggleRole(role: EngagementRole): void {
    const next = new Set(this.expandedRoles());
    if (next.has(role)) {
      next.delete(role);
    } else {
      next.add(role);
    }
    this.expandedRoles.set(next);
  }
}
