import { Component, computed, inject, signal } from '@angular/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { KanbanColumn } from '../kanban-column/kanban-column';
import { EngagementDetail } from '../engagement-detail/engagement-detail';
import { EngagementCard, EngagementColumn, ConsultantBadge, ClientBadge } from '../engagement.model';
import { Engagement, EngagementStatus } from '../../../../types/engagement.types';
import { EngagementRole } from '../../../../types/assignment.types';
import { EngagementService } from '../../../../services/engagement.service';

const COLUMN_STATUSES = [
  EngagementStatus.PLANNED,
  EngagementStatus.IN_PROGRESS,
  EngagementStatus.ON_HOLD,
  EngagementStatus.COMPLETED,
];

// TODO: replace with real lookups once ClientService/ConsultantService (via client + staffing gateway routes) are wired up.
// The engagement service only returns `clientId`, and consultant assignments live in the staffing service, so this
// deterministically fakes both from the engagement's id purely for display until those integrations land.
const PLACEHOLDER_CLIENTS: ClientBadge[] = [
  { companyName: 'Fidelity', initials: 'FI', color: '#4b9c5f' },
  { companyName: 'Vanguard', initials: 'VG', color: '#960b2f' },
  { companyName: 'BlackRock', initials: 'BR', color: '#000000' },
  { companyName: 'Charles Schwab', initials: 'CS', color: '#00a0df' },
  { companyName: 'PNC', initials: 'PNC', color: '#f58025' },
];

const PLACEHOLDER_CONSULTANTS: Omit<ConsultantBadge, 'projectRole'>[] = [
  { name: 'Jamie Lee', titleRole: 'Senior Associate', initials: 'JL', color: '#6366f1' },
  { name: 'Sam Rivera', titleRole: 'Associate', initials: 'SR', color: '#f59e0b' },
  { name: 'Alex Chen', titleRole: 'Lead Consultant', initials: 'AC', color: '#10b981' },
];

function toCard(engagement: Engagement): EngagementCard {
  const client = PLACEHOLDER_CLIENTS[engagement.clientId % PLACEHOLDER_CLIENTS.length];
  const consultant = PLACEHOLDER_CONSULTANTS[engagement.id % PLACEHOLDER_CONSULTANTS.length];

  return {
    ...engagement,
    client,
    consultants: [{ ...consultant, projectRole: EngagementRole.LEAD }],
  };
}

@Component({
  selector: 'app-kanban-board',
  imports: [KanbanColumn, EngagementDetail],
  templateUrl: './kanban-board.html',
  styleUrl: './kanban-board.css',
})
export class KanbanBoard {
  private readonly engagementService = inject(EngagementService);

  private readonly engagements = signal<Engagement[]>([]);

  protected readonly selected = signal<EngagementCard | null>(null);
  protected readonly connectedLists = COLUMN_STATUSES;

  constructor() {
    this.engagementService.getAll().subscribe({
      next: (engagements) => this.engagements.set(engagements),
      error: (err) => console.error('Failed to load engagements', err),
    });
  }

  protected readonly columns = computed<EngagementColumn[]>(() => {
    const cards = this.engagements().map(toCard);

    return COLUMN_STATUSES.map((status) => ({
      status,
      title: status,
      engagements: cards.filter((card) => card.status === status),
    }));
  });

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
}
