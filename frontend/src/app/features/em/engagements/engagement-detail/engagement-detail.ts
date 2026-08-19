import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { EngagementCard } from './engagement.model';
import { EditDatesModal } from '../editors/edit-dates-modal/edit-dates-modal';
import { EditableBadge } from '../editors/editable-badge/editable-badge';
import { EditableTitle } from '../editors/editable-title/editable-title';
import { EditableSummary } from '../editors/editable-summary/editable-summary';
import { ConsultantDetail } from '../../consultants/consultant-detail/consultant-detail';
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
  imports: [FormsModule, EditDatesModal, EditableBadge, EditableTitle, EditableSummary, ConsultantDetail],
  templateUrl: './engagement-detail.html',
  styleUrl: './engagement-detail.css',
})
export class EngagementDetail implements OnInit {
  private readonly assignmentService = inject(AssignmentService);
  private readonly consultantService = inject(ConsultantService);
  private readonly engagementService = inject(EngagementService);
  private readonly messageService = inject(MessageService);

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

  protected readonly consultantDetailVisible = signal(false);
  protected readonly selectedConsultant = signal<Consultant | null>(null);

  protected get isCreateMode(): boolean {
    return this.engagement === null;
  }

  protected get availableConsultants(): Consultant[] {
    const assignedIds = new Set(this.assignments().map((a) => a.consultantId));
    return this.allConsultants().filter((c) => !assignedIds.has(c.id));
  }

  // --- Delete: permanent, but always available — unstaffs any remaining assignments first. ---
  protected readonly deleteModalOpen = signal(false);
  protected readonly deleteHistory = signal<Assignment[] | null>(null);
  protected readonly historyLoading = signal(false);
  protected readonly deleting = signal(false);
  protected deleteConfirmName = '';

  protected get deleteNameConfirmed(): boolean {
    return this.engagement !== null && this.deleteConfirmName.trim() === this.engagement.engagementName;
  }

  protected openDeleteModal(): void {
    if (!this.engagement) {
      return;
    }

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
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to check assignment history. Please try again.',
        });
        console.error('Failed to load assignment history', err);
      },
    });
  }

  protected closeDeleteModal(): void {
    this.deleteModalOpen.set(false);
  }

  protected performDelete(): void {
    if (!this.engagement || !this.deleteNameConfirmed) {
      return;
    }

    this.deleting.set(true);

    this.engagementService.delete(this.engagement.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteModalOpen.set(false);
        this.delete.emit();
      },
      error: (err) => {
        this.deleting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Delete Failed',
          detail: err?.error?.message ?? 'Failed to delete engagement. Please try again.',
        });
        console.error(err);
      },
    });
  }

  // --- Cancel: the everyday "this isn't happening" action. Keeps the record and its history. ---
  protected readonly cancelModalOpen = signal(false);
  protected readonly cancelling = signal(false);

  protected get canCancel(): boolean {
    return this.engagement !== null && !NON_CANCELLABLE_STATUSES.has(this.engagement.status);
  }

  protected get staffedAssignments(): Assignment[] {
    return this.assignments().filter((a) => STAFFED_ASSIGNMENT_STATUSES.has(a.status));
  }

  protected openCancelModal(): void {
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

    this.engagementService.cancel(this.engagement.id).subscribe({
      next: (updated) => {
        this.cancelling.set(false);
        this.cancelModalOpen.set(false);
        this.cancelled.emit(updated);
      },
      error: (err) => {
        this.cancelling.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Cancel Failed',
          detail: err?.error?.message ?? 'Failed to cancel engagement. Please try again.',
        });
        console.error(err);
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
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Assign Failed',
            detail: err?.error?.message ?? 'Failed to assign consultant. Please try again.',
          });
          console.error('Failed to assign consultant', err);
        },
      });
  }

  protected initials(name: string): string {
    return initialsOf(name);
  }

  protected colorFor(name: string): string {
    return colorOf(name);
  }

  protected readonly unstaffTarget = signal<Assignment | null>(null);
  protected readonly unstaffing = signal(false);

  protected confirmUnstaff(assignment: Assignment): void {
    this.unstaffTarget.set(assignment);
  }

  protected cancelUnstaff(): void {
    this.unstaffTarget.set(null);
  }

  protected performUnstaff(): void {
    const target = this.unstaffTarget();
    if (!target) {
      return;
    }

    this.unstaffing.set(true);

    this.assignmentService.remove(target.id).subscribe({
      next: () => {
        this.unstaffing.set(false);
        this.unstaffTarget.set(null);
        this.loadConsultantsPanel();
      },
      error: (err) => {
        this.unstaffing.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Unstaff Failed',
          detail: err?.error?.message ?? 'Failed to unstaff consultant. Please try again.',
        });
        console.error(err);
      },
    });
  }

  protected openConsultant(consultantId: number): void {
    const consultant = this.allConsultants().find((c) => c.id === consultantId);
    if (!consultant) {
      return;
    }
    this.selectedConsultant.set(consultant);
    this.consultantDetailVisible.set(true);
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
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load staffing for this engagement.',
        });
        console.error(`Failed to load assignments for engagement ${engagementId}`, err);
      },
    });

    this.consultantService.getAll().subscribe({
      next: (consultants) => this.allConsultants.set(consultants),
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load consultants.',
        });
        console.error('Failed to load consultants', err);
      },
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
