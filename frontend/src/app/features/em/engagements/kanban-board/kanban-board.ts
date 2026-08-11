import { Component, computed, inject, signal } from '@angular/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { KanbanColumn } from '../kanban-column/kanban-column';
import { EngagementDetail } from '../engagement-detail/engagement-detail';
import { EngagementCard, EngagementColumn, ConsultantBadge, ClientBadge } from '../engagement-detail/engagement.model';
import { CreateEngagementRequest, Engagement, EngagementStatus, EngagementType } from '../../../../types/engagement.types';
import { Consultant } from '../../../../types/consultant.types';
import { EngagementService } from '../../../../services/engagement.service';
import { ConsultantService } from '../../../../services/consultant.service';
import { AssignmentService } from '../../../../services/assignment.service';
import { initialsOf, colorOf } from '../../../../shared/avatar';

const COLUMN_STATUSES = [
  EngagementStatus.PLANNED,
  EngagementStatus.IN_PROGRESS,
  EngagementStatus.ON_HOLD,
  EngagementStatus.COMPLETED,
];

// TODO: replace with a real lookup once ClientService (via the client gateway route) is wired up.
// The engagement service only returns `clientId`, and there's no client-lookup endpoint wired up yet,
// so this deterministically fakes the client badge from the engagement's id purely for display.
const PLACEHOLDER_CLIENTS: ClientBadge[] = [
  { companyName: 'Fidelity', initials: 'FI', color: '#4b9c5f' },
  { companyName: 'Vanguard', initials: 'VG', color: '#960b2f' },
  { companyName: 'BlackRock', initials: 'BR', color: '#000000' },
  { companyName: 'Charles Schwab', initials: 'CS', color: '#00a0df' },
  { companyName: 'PNC', initials: 'PNC', color: '#f58025' },
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

  private readonly engagements = signal<Engagement[]>([]);
  private readonly consultantsById = signal<Map<number, Consultant>>(new Map());
  private readonly consultantsByEngagement = signal<Map<number, ConsultantBadge[]>>(new Map());

  protected readonly selected = signal<EngagementCard | null>(null);
  protected readonly creatingStatus = signal<EngagementStatus | null>(null);
  protected readonly connectedLists = COLUMN_STATUSES;

  constructor() {
    this.consultantService.getAll().subscribe({
      next: (consultants) => this.consultantsById.set(new Map(consultants.map((c) => [c.id, c]))),
      error: (err) => console.error('Failed to load consultants', err),
    });

    this.engagementService.getAll().subscribe({
      next: (engagements) => {
        this.engagements.set(engagements);
        engagements.forEach((e) => this.refreshConsultants(e.id));
      },
      error: (err) => console.error('Failed to load engagements', err),
    });
  }

  protected readonly columns = computed<EngagementColumn[]>(() => {
    const badgesByEngagement = this.consultantsByEngagement();
    const cards = this.engagements().map((engagement) => this.toCard(engagement, badgesByEngagement));

    return COLUMN_STATUSES.map((status) => ({
      status,
      title: status,
      engagements: cards.filter((card) => card.status === status),
    }));
  });

  private toCard(engagement: Engagement, badgesByEngagement: Map<number, ConsultantBadge[]>): EngagementCard {
    const client = PLACEHOLDER_CLIENTS[engagement.clientId % PLACEHOLDER_CLIENTS.length];

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
      },
      error: (err) => console.error(`Failed to load consultants for engagement ${engagementId}`, err),
    });
  }

  protected drop(event: CdkDragDrop<EngagementCard[]>): void {
    const engagement = event.item.data as EngagementCard;
    const destinationStatus = event.container.id as EngagementStatus;

    if (engagement.status === destinationStatus) {
      return;
    }

    this.engagementService.updateStatus(engagement.id, destinationStatus).subscribe({
      next: () => {
        this.engagements.set(
          this.engagements().map((e) => (e.id === engagement.id ? { ...e, status: destinationStatus } : e)),
        );
      },
      error: (err) => console.error(`Failed to update engagement ${engagement.id} status`, err),
    });
  }

  protected selectEngagement(engagement: EngagementCard): void {
    this.selected.set(engagement);
  }

  protected closeDetail(): void {
    this.selected.set(null);
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
      error: (err) => console.error('Failed to create engagement', err),
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
      error: (err) => console.error(`Failed to update engagement ${id} summary`, err),
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
      error: (err) => console.error(`Failed to update engagement ${id} name`, err),
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
      error: (err) => console.error(`Failed to update engagement ${id} dates`, err),
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
      error: (err) => console.error(`Failed to update engagement ${id} type`, err),
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
      error: (err) => console.error(`Failed to update engagement ${id} status`, err),
    });
  }
}
