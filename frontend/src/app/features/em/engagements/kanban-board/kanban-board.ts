import { Component, signal } from '@angular/core';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { KanbanColumn } from '../kanban-column/kanban-column';
import { EngagementDetail } from '../engagement-detail/engagement-detail';
import { EngagementCard, EngagementColumn } from '../engagement.model';
import { EngagementStatus, EngagementType } from '../../../../types/engagement.types';

const CLIENTS = {
  fidelity: { companyName: 'Fidelity', initials: 'FI', color: '#4b9c5f' },
  vanguard: { companyName: 'Vanguard', initials: 'VG', color: '#960b2f' },
  blackrock: { companyName: 'BlackRock', initials: 'BR', color: '#000000' },
  charlesSchwab: { companyName: 'Charles Schwab', initials: 'CS', color: '#00a0df' },
  pnc: { companyName: 'PNC', initials: 'PNC', color: '#f58025' },
} as const;

const CONSULTANTS = {
  jamie: { name: 'Jamie Lee', titleRole: 'Senior Associate', initials: 'JL', color: '#6366f1' },
  sam: { name: 'Sam Rivera', titleRole: 'Associate', initials: 'SR', color: '#f59e0b' },
  alex: { name: 'Alex Chen', titleRole: 'Lead Consultant', initials: 'AC', color: '#10b981' },
  priya: { name: 'Priya Nair', titleRole: 'Senior Associate', initials: 'PN', color: '#ec4899' },
  jordan: { name: 'Jordan Blake', titleRole: 'Associate', initials: 'JB', color: '#3b82f6' },
  taylor: { name: 'Taylor Kim', titleRole: 'Senior Associate', initials: 'TK', color: '#a855f7' },
  morgan: { name: 'Morgan Reyes', titleRole: 'Lead Consultant', initials: 'MR', color: '#ef4444' },
  casey: { name: 'Casey Nguyen', titleRole: 'Associate', initials: 'CN', color: '#14b8a6' },
} as const;

const now = '2026-08-07T00:00:00Z';

@Component({
  selector: 'app-kanban-board',
  imports: [KanbanColumn, EngagementDetail],
  templateUrl: './kanban-board.html',
  styleUrl: './kanban-board.css',
})
export class KanbanBoard {
  protected readonly selected = signal<EngagementCard | null>(null);

  protected readonly columns: EngagementColumn[] = [
    {
      status: EngagementStatus.PLANNED,
      title: 'Planned',
      engagements: [
        {
          id: 1,
          engagementName: 'ERP Rollout',
          clientId: 1,
          engagementType: EngagementType.RISK_CONSULTING,
          startDate: '2026-09-01',
          targetEndDate: '2027-01-15',
          status: EngagementStatus.PLANNED,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.fidelity,
          consultants: [CONSULTANTS.jamie, CONSULTANTS.sam],
        },
        {
          id: 2,
          engagementName: 'Data Migration',
          clientId: 2,
          engagementType: EngagementType.RISK_CONSULTING,
          startDate: '2026-09-15',
          targetEndDate: '2026-12-01',
          status: EngagementStatus.PLANNED,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.vanguard,
          consultants: [CONSULTANTS.sam],
        },
      ],
    },
    {
      status: EngagementStatus.IN_PROGRESS,
      title: 'In Progress',
      engagements: [
        {
          id: 3,
          engagementName: 'Cloud Modernization',
          clientId: 3,
          engagementType: EngagementType.RISK_CONSULTING,
          startDate: '2026-06-01',
          targetEndDate: '2026-11-30',
          status: EngagementStatus.IN_PROGRESS,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.blackrock,
          consultants: [CONSULTANTS.alex, CONSULTANTS.priya, CONSULTANTS.jordan],
        },
        {
          id: 4,
          engagementName: 'Security Audit',
          clientId: 4,
          engagementType: EngagementType.AUDIT,
          startDate: '2026-07-01',
          targetEndDate: '2026-10-01',
          status: EngagementStatus.IN_PROGRESS,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.charlesSchwab,
          consultants: [CONSULTANTS.priya],
        },
        {
          id: 5,
          engagementName: 'API Integration',
          clientId: 5,
          engagementType: EngagementType.RISK_CONSULTING,
          startDate: '2026-05-15',
          targetEndDate: '2026-09-30',
          status: EngagementStatus.IN_PROGRESS,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.pnc,
          consultants: [CONSULTANTS.jordan, CONSULTANTS.taylor],
        },
      ],
    },
    {
      status: EngagementStatus.ON_HOLD,
      title: 'On Hold',
      engagements: [
        {
          id: 6,
          engagementName: 'Legacy Decommission',
          clientId: 1,
          engagementType: EngagementType.TAX_ADVISORY,
          startDate: '2026-04-01',
          targetEndDate: '2026-08-01',
          status: EngagementStatus.ON_HOLD,
          active: true,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.fidelity,
          consultants: [CONSULTANTS.taylor, CONSULTANTS.morgan],
        },
      ],
    },
    {
      status: EngagementStatus.COMPLETED,
      title: 'Completed',
      engagements: [
        {
          id: 7,
          engagementName: 'Payroll Upgrade',
          clientId: 2,
          engagementType: EngagementType.FINANCIAL_ADVISORY,
          startDate: '2026-01-01',
          targetEndDate: '2026-05-01',
          status: EngagementStatus.COMPLETED,
          active: false,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.vanguard,
          consultants: [CONSULTANTS.morgan],
        },
        {
          id: 8,
          engagementName: 'Onboarding Portal',
          clientId: 3,
          engagementType: EngagementType.RISK_CONSULTING,
          startDate: '2026-02-01',
          targetEndDate: '2026-06-01',
          status: EngagementStatus.COMPLETED,
          active: false,
          createdAt: now,
          updatedAt: now,
          client: CLIENTS.blackrock,
          consultants: [CONSULTANTS.casey, CONSULTANTS.jamie, CONSULTANTS.sam],
        },
      ],
    },
  ];

  protected readonly connectedLists = this.columns.map((c) => c.status);

  protected drop(event: CdkDragDrop<EngagementCard[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex,
    );
  }

  protected selectEngagement(engagement: EngagementCard): void {
    this.selected.set(engagement);
  }

  protected closeDetail(): void {
    this.selected.set(null);
  }
}
