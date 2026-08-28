import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { MessageService } from 'primeng/api';
import { KanbanColumn } from '../kanban-column/kanban-column';
import { EngagementDetail } from '../engagement-detail/engagement-detail';
import { EngagementCard, EngagementColumn, ConsultantBadge, ClientBadge } from '../engagement-detail/engagement.model';
import { CreateEngagementRequest, Engagement, EngagementStatus, EngagementType } from '../../../../types/engagement.types';
import { Consultant } from '../../../../types/consultant.types';
import { Client } from '../../../../types/client.types';
import { EngagementService } from '../../../../services/engagement.service';
import { ConsultantService } from '../../../../services/consultant.service';
import { AssignmentService } from '../../../../services/assignment.service';
import { ClientService } from '../../../../services/ClientService';
import { initialsOf, colorOf } from '../../../../shared/avatar';
import { engagementWarnings } from '../engagement-warnings';

type StaffingFilter = 'all' | 'staffed' | 'unstaffed';
type SortBy = 'targetEndDate' | 'startDate' | 'name' | 'client' | 'staffing' | 'warnings' | 'updatedAt';

const COLUMN_STATUSES = [
  EngagementStatus.PLANNED,
  EngagementStatus.IN_PROGRESS,
  EngagementStatus.ON_HOLD,
  EngagementStatus.COMPLETED, 
  EngagementStatus.CANCELLED,
];

@Component({
  selector: 'app-kanban-board',
  imports: [KanbanColumn, EngagementDetail],
  templateUrl: './kanban-board.html',
  styleUrl: './kanban-board.css',
})
export class KanbanBoard {
  private readonly engagementService = inject(EngagementService);
  private readonly consultantService = inject(ConsultantService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly clientService = inject(ClientService);
  private readonly messageService = inject(MessageService);
  private readonly route = inject(ActivatedRoute);

  private readonly engagements = signal<Engagement[]>([]);
  private readonly consultantsById = signal<Map<number, Consultant>>(new Map());
  private readonly consultantsByEngagement = signal<Map<number, ConsultantBadge[]>>(new Map());
  private readonly consultantIdsByEngagement = signal<Map<number, Set<number>>>(new Map());
  private readonly clientsById = signal<Map<number, Client>>(new Map());

  protected readonly selected = signal<EngagementCard | null>(null);
  protected readonly creatingStatus = signal<EngagementStatus | null>(null);
  protected readonly connectedLists = COLUMN_STATUSES;
  protected readonly cancelledStatus = EngagementStatus.CANCELLED;
  protected readonly engagementTypes = Object.values(EngagementType);

  // --- Filters (client-side only; applied before grouping into columns) ---
  protected readonly hiddenClientIds = signal<Set<number>>(new Set());
  protected readonly hiddenTypes = signal<Set<EngagementType>>(new Set());
  protected readonly onlyWithWarnings = signal(false);
  protected readonly staffingFilter = signal<StaffingFilter>('all');
  protected readonly consultantFilter = signal<number | null>(null);
  protected readonly dateRangeStart = signal('');
  protected readonly dateRangeEnd = signal('');
  protected readonly filtersModalOpen = signal(false);

  // --- Sorting (client-side only; applied within each column after filtering) ---
  protected readonly sortBy = signal<SortBy>('targetEndDate');

  protected setSortBy(value: string): void {
    this.sortBy.set(value as SortBy);
  }

  protected readonly consultantOptions = computed<Consultant[]>(() =>
    Array.from(this.consultantsById().values()).sort((a, b) => a.name.localeCompare(b.name)),
  );

  protected readonly activeFilterCount = computed<number>(() => {
    let count = this.hiddenClientIds().size + this.hiddenTypes().size;
    if (this.onlyWithWarnings()) count++;
    if (this.staffingFilter() !== 'all') count++;
    if (this.consultantFilter() !== null) count++;
    if (this.dateRangeStart()) count++;
    if (this.dateRangeEnd()) count++;
    return count;
  });

  private readonly pendingOpenId = signal<number | null>(null);

  constructor() {
    // Re-run whenever the search bar navigates here with a new ?openId=,
    // even if we're already sitting on this route (component isn't re-created).
    this.route.queryParamMap.subscribe((params) => {
      this.pendingOpenId.set(Number(params.get('openId')) || null);
    });

    effect(() => {
      const openId = this.pendingOpenId();
      if (!openId) return;

      const card = this.columns()
        .flatMap((column) => column.engagements)
        .find((c) => c.id === openId);

      if (card) {
        this.selected.set(card);
        this.pendingOpenId.set(null);
      }
    });

    this.consultantService.getAll().subscribe({
      next: (consultants) => this.consultantsById.set(new Map(consultants.map((c) => [c.id, c]))),
      error: (err) => this.notifyError('Failed to load consultants.', err),
    });

    this.clientService.getAllClients(0, 100).subscribe({
      next: (page) => this.clientsById.set(new Map(page.content.map((c) => [c.id!, c]))),
      error: (err) => this.notifyError('Failed to load clients.', err),
    });

    this.engagementService.getAll().subscribe({
      next: (engagements) => {
        this.engagements.set(engagements);
        engagements.forEach((e) => this.refreshConsultants(e.id));
      },
      error: (err) => this.notifyError('Failed to load engagements.', err),
    });
  }

  protected readonly clients = computed<Client[]>(() => Array.from(this.clientsById().values()));

  private readonly filteredCards = computed<EngagementCard[]>(() => {
    const badgesByEngagement = this.consultantsByEngagement();
    const clientsById = this.clientsById();
    const consultantIdsByEngagement = this.consultantIdsByEngagement();

    const hiddenClientIds = this.hiddenClientIds();
    const hiddenTypes = this.hiddenTypes();
    const onlyWithWarnings = this.onlyWithWarnings();
    const staffingFilter = this.staffingFilter();
    const consultantFilter = this.consultantFilter();
    const rangeStart = this.dateRangeStart();
    const rangeEnd = this.dateRangeEnd();

    return this.engagements()
      .map((engagement) => this.toCard(engagement, badgesByEngagement, clientsById))
      .filter((card) => {
        if (hiddenClientIds.has(card.clientId)) return false;
        if (hiddenTypes.has(card.engagementType)) return false;

        if (staffingFilter === 'staffed' && card.consultants.length === 0) return false;
        if (staffingFilter === 'unstaffed' && card.consultants.length > 0) return false;

        if (consultantFilter !== null && !consultantIdsByEngagement.get(card.id)?.has(consultantFilter)) return false;

        if (onlyWithWarnings) {
          const warnings = engagementWarnings({
            status: card.status,
            startDate: card.startDate,
            targetEndDate: card.targetEndDate,
            consultantCount: card.consultants.length,
          });
          if (warnings.length === 0) return false;
        }

        if (rangeStart && card.targetEndDate < rangeStart) return false;
        if (rangeEnd && card.startDate > rangeEnd) return false;

        return true;
      });
  });

  protected readonly columns = computed<EngagementColumn[]>(() => {
    const cards = this.filteredCards();
    const sortBy = this.sortBy();

    return COLUMN_STATUSES.map((status) => ({
      status,
      title: status,
      engagements: cards.filter((card) => card.status === status).sort((a, b) => this.compareCards(a, b, sortBy)),
    }));
  });

  protected downloadCsv(): void {
    const header = ['Engagement Name', 'Client', 'Status', 'Type', 'Start Date', 'Target End Date', 'Consultants', 'Has Warnings'];

    const rows = this.columns()
      .flatMap((column) => column.engagements)
      .map((card) => [
        card.engagementName,
        card.client.companyName,
        card.status,
        card.engagementType,
        card.startDate,
        card.targetEndDate,
        String(card.consultants.length),
        this.hasWarnings(card) ? 'Yes' : 'No',
      ]);

    const escapeCell = (value: string) => `"${value.replace(/"/g, '""')}"`;
    const csv = [header, ...rows].map((row) => row.map(escapeCell).join(',')).join('\r\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `engagements-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private hasWarnings(card: EngagementCard): boolean {
    return (
      engagementWarnings({
        status: card.status,
        startDate: card.startDate,
        targetEndDate: card.targetEndDate,
        consultantCount: card.consultants.length,
      }).length > 0
    );
  }

  private compareCards(a: EngagementCard, b: EngagementCard, sortBy: SortBy): number {
    switch (sortBy) {
      case 'targetEndDate':
        return a.targetEndDate.localeCompare(b.targetEndDate);
      case 'startDate':
        return a.startDate.localeCompare(b.startDate);
      case 'name':
        return a.engagementName.localeCompare(b.engagementName);
      case 'client':
        return a.client.companyName.localeCompare(b.client.companyName);
      case 'staffing':
        return a.consultants.length - b.consultants.length;
      case 'warnings':
        return Number(this.hasWarnings(b)) - Number(this.hasWarnings(a));
      case 'updatedAt':
        return b.updatedAt.localeCompare(a.updatedAt);
    }
  }

  protected toggleClientFilter(clientId: number): void {
    const next = new Set(this.hiddenClientIds());
    if (next.has(clientId)) {
      next.delete(clientId);
    } else {
      next.add(clientId);
    }
    this.hiddenClientIds.set(next);
  }

  protected isClientHidden(clientId: number): boolean {
    return this.hiddenClientIds().has(clientId);
  }

  protected showAllClients(): void {
    this.hiddenClientIds.set(new Set());
  }

  protected hideAllClients(): void {
    this.hiddenClientIds.set(new Set(this.clients().map((c) => c.id!)));
  }

  protected toggleTypeFilter(type: EngagementType): void {
    const next = new Set(this.hiddenTypes());
    if (next.has(type)) {
      next.delete(type);
    } else {
      next.add(type);
    }
    this.hiddenTypes.set(next);
  }

  protected isTypeHidden(type: EngagementType): boolean {
    return this.hiddenTypes().has(type);
  }

  protected setStaffingFilter(value: StaffingFilter): void {
    this.staffingFilter.set(value);
  }

  protected setConsultantFilter(value: string): void {
    this.consultantFilter.set(value ? Number(value) : null);
  }

  protected resetDateRange(): void {
    this.dateRangeStart.set('');
    this.dateRangeEnd.set('');
  }

  protected resetAllFilters(): void {
    this.hiddenClientIds.set(new Set());
    this.hiddenTypes.set(new Set());
    this.onlyWithWarnings.set(false);
    this.staffingFilter.set('all');
    this.consultantFilter.set(null);
    this.resetDateRange();
  }

  private toCard(
    engagement: Engagement,
    badgesByEngagement: Map<number, ConsultantBadge[]>,
    clientsById: Map<number, Client>,
  ): EngagementCard {
    const companyName = clientsById.get(engagement.clientId)?.companyName ?? 'Unknown Client';
    const client: ClientBadge = {
      companyName,
      initials: initialsOf(companyName),
      color: colorOf(companyName),
    };

    return {
      ...engagement,
      client,
      consultants: badgesByEngagement.get(engagement.id) ?? [],
    };
  }

  protected refreshConsultants(engagementId: number): void {
    this.assignmentService.getByEngagement(engagementId).subscribe({
      next: (assignments) => {
        const consultantsById = this.consultantsById();
        const badges: ConsultantBadge[] = assignments.map((a) => ({
          name: a.consultantName,
          titleRole: consultantsById.get(a.consultantId)?.titleRole ?? '',
          initials: initialsOf(a.consultantName),
          color: colorOf(a.consultantName),
          projectRole: a.engagementRole,
        }));

        const next = new Map(this.consultantsByEngagement());
        next.set(engagementId, badges);
        this.consultantsByEngagement.set(next);

        const nextIds = new Map(this.consultantIdsByEngagement());
        nextIds.set(engagementId, new Set(assignments.map((a) => a.consultantId)));
        this.consultantIdsByEngagement.set(nextIds);
      },
      error: (err) => this.notifyError('Failed to load consultants for an engagement.', err),
    });
  }

  protected drop(event: CdkDragDrop<EngagementCard[]>): void {
    const engagement = event.item.data as EngagementCard;
    const destinationStatus = event.container.id as EngagementStatus;

    if (engagement.status === destinationStatus) {
      return;
    }

    const request$ =
      destinationStatus === EngagementStatus.CANCELLED
        ? this.engagementService.cancel(engagement.id)
        : this.engagementService.updateStatus(engagement.id, destinationStatus);

    request$.subscribe({
      next: (updated) => {
        this.engagements.set(
          this.engagements().map((e) => (e.id === engagement.id ? { ...e, status: updated.status } : e)),
        );
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Move Failed',
          detail: err?.error?.message ?? 'Failed to update the engagement status. Please try again.',
        });
        console.error(err);
      },
    });
  }

  protected selectEngagement(engagement: EngagementCard): void {
    this.selected.set(engagement);
  }

  protected closeDetail(): void {
    this.selected.set(null);
  }

  protected onEngagementDeleted(id: number): void {
    this.engagements.set(this.engagements().filter((e) => e.id !== id));
    this.selected.set(null);
  }

  protected onEngagementCancelled(updated: Engagement): void {
    this.engagements.set(this.engagements().map((e) => (e.id === updated.id ? updated : e)));

    const current = this.selected();
    if (current && current.id === updated.id) {
      this.selected.set({ ...current, status: updated.status });
    }
  }

  protected startCreate(status: EngagementStatus): void {
    this.creatingStatus.set(status);
  }

  protected cancelCreate(): void {
    this.creatingStatus.set(null);
  }

  protected submitCreate(request: CreateEngagementRequest): void {
    this.engagementService.create(request).subscribe({
      next: (engagement) => {
        this.engagements.set([...this.engagements(), engagement]);
        this.creatingStatus.set(null);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Create Failed',
          detail: err?.error?.message ?? 'Failed to create engagement. Please try again.',
        });
        console.error(err);
      },
    });
  }

  protected updateSummary(id: number, summary: string): void {
    this.engagementService.update(id, { summary }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));

        const current = this.selected();
        if (current && current.id === id) {
          this.selected.set({ ...current, summary: updated.summary });
        }
      },
      error: (err) => this.notifyUpdateError('summary', id, err),
    });
  }

  protected updateName(id: number, engagementName: string): void {
    this.engagementService.update(id, { engagementName }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));

        const current = this.selected();
        if (current && current.id === id) {
          this.selected.set({ ...current, engagementName: updated.engagementName });
        }
      },
      error: (err) => this.notifyUpdateError('name', id, err),
    });
  }

  protected updateDates(id: number, dates: { startDate: string; targetEndDate: string }): void {
    this.engagementService.update(id, dates).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));

        const current = this.selected();
        if (current && current.id === id) {
          this.selected.set({ ...current, startDate: updated.startDate, targetEndDate: updated.targetEndDate });
        }
      },
      error: (err) => this.notifyUpdateError('dates', id, err),
    });
  }

  protected updateType(id: number, engagementType: EngagementType): void {
    this.engagementService.update(id, { engagementType }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));

        const current = this.selected();
        if (current && current.id === id) {
          this.selected.set({ ...current, engagementType: updated.engagementType });
        }
      },
      error: (err) => this.notifyUpdateError('type', id, err),
    });
  }

  protected updateStatus(id: number, status: EngagementStatus): void {
    this.engagementService.updateStatus(id, status).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));

        const current = this.selected();
        if (current && current.id === id) {
          this.selected.set({ ...current, status: updated.status });
        }
      },
      error: (err) => this.notifyUpdateError('status', id, err),
    });
  }

  private notifyError(detail: string, err: unknown): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail,
    });
    console.error(detail, err);
  }

  private notifyUpdateError(field: string, engagementId: number, err: unknown): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Update Failed',
      detail: (err as { error?: { message?: string } })?.error?.message ?? `Failed to update the engagement ${field}. Please try again.`,
    });
    console.error(`Failed to update engagement ${engagementId} ${field}`, err);
  }
}
