import { Component, effect, inject, input, model, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AssignmentService } from '../../../../services/assignment.service';
import { EngagementService } from '../../../../services/engagement.service';
import { colorOf, initialsOf } from '../../../../shared/avatar';
import { Assignment } from '../../../../types/assignment.types';
import { Consultant } from '../../../../types/consultant.types';
import { EngagementStatus } from '../../../../types/engagement.types';
import { AssignEngagementForm } from './assign-engagement-form/assign-engagement-form';

// NOTE: assignments previously carried their own independently-editable `status`/`statusOverridden`
// (see Assignment type + assignment.service.updateStatus). For MVP, the badge shown here is derived
// entirely from the parent engagement's status instead — the per-assignment fields are left in the
// backend/model archived but unused by this view.
export interface AssignmentRow {
  id: number;
  engagementId: number;
  engagementName: string;
  engagementRole: string;
  status: EngagementStatus;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-consultant-detail',
  imports: [AssignEngagementForm],
  templateUrl: './consultant-detail.html',
  styleUrl: './consultant-detail.css',
})
export class ConsultantDetail {
  private readonly assignmentService = inject(AssignmentService);
  private readonly engagementService = inject(EngagementService);

  readonly consultant = input<Consultant | null>(null);
  readonly visible = model<boolean>(false);

  readonly assignments = signal<AssignmentRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly everOpened = signal(false);
  readonly assignFormVisible = signal(false);

  /**
   * Mirrors `visible()`, except on the very first open it's held at `false`
   * for one extra frame after the panel is mounted (`everOpened` flips true)
   * so the browser paints the off-screen state before we transition it
   * on-screen — otherwise the slide-in CSS transition has no prior state
   * to animate from and the panel just appears.
   */
  readonly panelVisible = signal(false);

  readonly initialsOf = initialsOf;
  readonly colorOf = colorOf;
  readonly EngagementStatus = EngagementStatus;

  private static readonly STATUS_CLASSES: Record<EngagementStatus, string> = {
    [EngagementStatus.PLANNED]: 'bg-amber-100 text-amber-700',
    [EngagementStatus.IN_PROGRESS]: 'bg-green-100 text-green-700',
    [EngagementStatus.ON_HOLD]: 'bg-orange-100 text-orange-700',
    [EngagementStatus.COMPLETED]: 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]',
    [EngagementStatus.CANCELLED]: 'bg-red-100 text-red-700',
  };

  statusClasses(status: EngagementStatus): string {
    return ConsultantDetail.STATUS_CLASSES[status] ?? 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]';
  }

  private static readonly PROGRESS_FILL_CLASSES: Record<EngagementStatus, string> = {
    [EngagementStatus.PLANNED]: 'bg-[var(--p-surface-500)]',
    [EngagementStatus.IN_PROGRESS]: 'bg-blue-500',
    [EngagementStatus.ON_HOLD]: 'bg-orange-400',
    [EngagementStatus.COMPLETED]: 'bg-emerald-500',
    [EngagementStatus.CANCELLED]: 'bg-[var(--p-surface-400)]',
  };

  progressFillClasses(status: EngagementStatus): string {
    return ConsultantDetail.PROGRESS_FILL_CLASSES[status] ?? 'bg-[var(--p-surface-500)]';
  }

  progressPercent(row: AssignmentRow): number {
    if (row.status === EngagementStatus.COMPLETED) return 100;
    if (row.status === EngagementStatus.PLANNED) return 0;
    if (!row.endDate) return 0;

    const start = new Date(row.startDate).getTime();
    const end = new Date(row.endDate).getTime();
    if (end <= start) return 100;

    const elapsed = ((Date.now() - start) / (end - start)) * 100;
    return Math.min(100, Math.max(0, elapsed));
  }

  constructor() {
    effect(() => {
      const consultant = this.consultant();

      if (this.visible() && consultant) {
        this.loadAssignments(consultant.id);

        if (!this.everOpened()) {
          this.everOpened.set(true);
          requestAnimationFrame(() => this.panelVisible.set(true));
        } else {
          this.panelVisible.set(true);
        }
      } else {
        this.panelVisible.set(false);
      }
    });
  }

  close(): void {
    this.visible.set(false);
  }

  openAssignForm(): void {
    this.assignFormVisible.set(true);
  }

  onAssigned(_assignment: Assignment): void {
    const consultant = this.consultant();
    if (consultant) this.loadAssignments(consultant.id);
  }

  private loadAssignments(consultantId: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.assignmentService
      .getByConsultant(consultantId)
      .pipe(
        switchMap((assignments) => {
          if (assignments.length === 0) return of([]);

          const rows = assignments.map((assignment) =>
            this.engagementService.getById(assignment.engagementId).pipe(
              map(
                (engagement): AssignmentRow => ({
                  id: assignment.id,
                  engagementId: assignment.engagementId,
                  engagementName: engagement.engagementName,
                  engagementRole: assignment.engagementRole,
                  status: engagement.status,
                  startDate: assignment.assignmentStartDate,
                  endDate: assignment.assignmentEndDate,
                }),
              ),
              catchError(() =>
                of<AssignmentRow>({
                  id: assignment.id,
                  engagementId: assignment.engagementId,
                  engagementName: 'Unknown Engagement',
                  engagementRole: assignment.engagementRole,
                  status: EngagementStatus.CANCELLED,
                  startDate: assignment.assignmentStartDate,
                  endDate: assignment.assignmentEndDate,
                }),
              ),
            ),
          );

          return forkJoin(rows);
        }),
      )
      .subscribe({
        next: (rows) => {
          this.assignments.set(rows);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load assignments.');
          this.loading.set(false);
        },
      });
  }
}
