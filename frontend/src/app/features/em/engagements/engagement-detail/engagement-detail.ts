import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EngagementCard } from './engagement.model';
import { EditDatesModal } from '../editors/edit-dates-modal/edit-dates-modal';
import { EditableBadge } from '../editors/editable-badge/editable-badge';
import { EditableTitle } from '../editors/editable-title/editable-title';
import { EditableSummary } from '../editors/editable-summary/editable-summary';
import { Assignment, AssignmentStatus, EngagementRole } from '../../../../types/assignment.types';
import { Consultant } from '../../../../types/consultant.types';
import { Client } from '../../../../types/client.types';
import { CreateEngagementRequest, Engagement, EngagementStatus, EngagementType } from '../../../../types/engagement.types';
import { AssignmentService } from '../../../../services/assignment.service';
import { ConsultantService } from '../../../../services/consultant.service';
import { EngagementService } from '../../../../services/engagement.service';
import { initialsOf, colorOf } from '../../../../shared/avatar';
import { engagementStatusIcon } from '../engagement-status-icon';

const NON_CANCELLABLE_STATUSES = new Set<EngagementStatus>([EngagementStatus.COMPLETED, EngagementStatus.CANCELLED]);
const STAFFED_ASSIGNMENT_STATUSES = new Set<AssignmentStatus>([AssignmentStatus.ACTIVE, AssignmentStatus.PENDING]);

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

const ROLE_ORDER: Record<EngagementRole, number> = {
  [EngagementRole.LEAD]: 0,
  [EngagementRole.SENIOR_ASSOCIATE]: 1,
  [EngagementRole.ASSOCIATE]: 2,
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
  private readonly engagementService = inject(EngagementService);

  @Input() engagement: EngagementCard | null = null;
  @Input() initialStatus: EngagementStatus = EngagementStatus.PLANNED;
  @Input() clients: Client[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() create = new EventEmitter<CreateEngagementRequest>();
  @Output() updateSummary = new EventEmitter<string>();
  @Output() updateName = new EventEmitter<string>();
  @Output() updateDates = new EventEmitter<{ startDate: string; targetEndDate: string }>();
  @Output() updateType = new EventEmitter<EngagementType>();
  @Output() updateStatus = new EventEmitter<EngagementStatus>();
  @Output() assigned = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<Engagement>();

  protected readonly engagementTypes = Object.values(EngagementType);
  protected readonly statuses = Object.values(EngagementStatus);
  /** Cancellation has its own dedicated flow (with a staffing-impact preview) — never a casual badge edit. */
  protected readonly editableStatuses = this.statuses.filter((s) => s !== EngagementStatus.CANCELLED);
  protected readonly engagementRoles = Object.values(EngagementRole);
  protected readonly statusIcon = engagementStatusIcon;

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
    })).sort((a, b) => ROLE_ORDER[a.role] - ROLE_ORDER[b.role]);
  });

  protected form: CreateEngagementRequest = this.emptyForm();

  protected editingDates = false;
  protected menuOpen = false;

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

  // --- Delete: reserved for engagements with zero staffing footprint, ever. ---
  protected readonly deleteModalOpen = signal(false);
  protected readonly deleteHistory = signal<Assignment[] | null>(null);
  protected readonly historyLoading = signal(false);
  protected readonly deleting = signal(false);
  protected readonly deleteError = signal<string | null>(null);
  protected deleteConfirmName = '';

  protected get canDelete(): boolean {
    const history = this.deleteHistory();
    return history !== null && history.length === 0;
  }

  protected get deleteNameConfirmed(): boolean {
    return this.engagement !== null && this.deleteConfirmName.trim() === this.engagement.engagementName;
  }

  protected openDeleteModal(): void {
    if (!this.engagement) {
      return;
    }

    this.deleteError.set(null);
    this.deleteConfirmName = '';
    this.deleteHistory.set(null);
    this.deleteModalOpen.set(true);

    this.historyLoading.set(true);
    this.assignmentService.getHistoryByEngagement(this.engagement.id).subscribe({
      next: (history) => {
        this.deleteHistory.set(history);
        this.historyLoading.set(false);
      },
      error: (err) => {
        this.historyLoading.set(false);
        this.deleteError.set('Failed to check assignment history. Please try again.');
        console.error('Failed to load assignment history', err);
      },
    });
  }

  protected closeDeleteModal(): void {
    this.deleteModalOpen.set(false);
  }

  protected performDelete(): void {
    if (!this.engagement || !this.canDelete || !this.deleteNameConfirmed) {
      return;
    }

    this.deleting.set(true);
    this.deleteError.set(null);

    this.engagementService.delete(this.engagement.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteModalOpen.set(false);
        this.delete.emit();
      },
      error: (err) => {
        this.deleting.set(false);
        this.deleteError.set(err?.error?.message ?? 'Failed to delete engagement. Please try again.');
      },
    });
  }

  // --- Cancel: the everyday "this isn't happening" action. Keeps the record and its history. ---
  protected readonly cancelModalOpen = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly cancelError = signal<string | null>(null);

  protected get canCancel(): boolean {
    return this.engagement !== null && !NON_CANCELLABLE_STATUSES.has(this.engagement.status);
  }

  protected get staffedAssignments(): Assignment[] {
    return this.assignments().filter((a) => STAFFED_ASSIGNMENT_STATUSES.has(a.status));
  }

  protected openCancelModal(): void {
    this.cancelError.set(null);
    this.cancelModalOpen.set(true);
  }

  protected closeCancelModal(): void {
    this.cancelModalOpen.set(false);
  }

  protected performCancel(): void {
    if (!this.engagement || !this.canCancel) {
      return;
    }

    this.cancelling.set(true);
    this.cancelError.set(null);

    this.engagementService.cancel(this.engagement.id).subscribe({
      next: (updated) => {
        this.cancelling.set(false);
        this.cancelModalOpen.set(false);
        this.cancelled.emit(updated);
      },
      error: (err) => {
        this.cancelling.set(false);
        this.cancelError.set(err?.error?.message ?? 'Failed to cancel engagement. Please try again.');
      },
    });
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
        assignmentEndDate: this.engagement.targetEndDate,
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
