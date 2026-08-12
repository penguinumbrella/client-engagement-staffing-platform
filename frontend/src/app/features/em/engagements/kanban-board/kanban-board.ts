import { Component, computed, inject, signal } from '@angular/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
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

  private readonly engagements = signal<Engagement[]>([]);
  private readonly consultantsById = signal<Map<number, Consultant>>(new Map());
  private readonly consultantsByEngagement = signal<Map<number, ConsultantBadge[]>>(new Map());
  private readonly clientsById = signal<Map<number, Client>>(new Map());

  protected readonly selected = signal<EngagementCard | null>(null);
  protected readonly creatingStatus = signal<EngagementStatus | null>(null);
  protected readonly connectedLists = COLUMN_STATUSES;
  protected readonly cancelledStatus = EngagementStatus.CANCELLED;

  constructor() {
    this.consultantService.getAll().subscribe({
      next: (consultants) => this.consultantsById.set(new Map(consultants.map((c) => [c.id, c]))),
      error: (err) => console.error('Failed to load consultants', err),
    });

    this.clientService.getAllClients().subscribe({
      next: (page) => this.clientsById.set(new Map(page.content.map((c) => [c.id!, c]))),
      error: (err) => console.error('Failed to load clients', err),
    });

    this.engagementService.getAll().subscribe({
      next: (engagements) => {
        this.engagements.set(engagements);
        engagements.forEach((e) => this.refreshConsultants(e.id));
      },
      error: (err) => console.error('Failed to load engagements', err),
    });
  }

  protected readonly clients = computed<Client[]>(() => Array.from(this.clientsById().values()));

  protected readonly columns = computed<EngagementColumn[]>(() => {
    const badgesByEngagement = this.consultantsByEngagement();
    const clientsById = this.clientsById();
    const cards = this.engagements().map((engagement) => this.toCard(engagement, badgesByEngagement, clientsById));

    return COLUMN_STATUSES.map((status) => ({
      status,
      title: status,
      engagements: cards.filter((card) => card.status === status),
    }));
  });

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
      error: (err) => console.error(`Failed to update engagement ${engagement.id} status`, err),
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
