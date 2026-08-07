import { Component, signal } from '@angular/core';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

interface Engagement {
  id: number;
  name: string;
  client: string;
  consultant: string;
}

interface Column {
  status: string;
  title: string;
  engagements: Engagement[];
}

@Component({
  selector: 'app-engagements',
  imports: [DragDropModule],
  templateUrl: './engagements.html',
  styleUrl: './engagements.css',
})
export class Engagements {
  protected readonly selected = signal<Engagement | null>(null);

  protected readonly columns: Column[] = [
    {
      status: 'planned',
      title: 'Planned',
      engagements: [
        { id: 1, name: 'ERP Rollout', client: 'Acme Corp', consultant: 'Jamie Lee' },
        { id: 2, name: 'Data Migration', client: 'Globex', consultant: 'Sam Rivera' },
      ],
    },
    {
      status: 'in-progress',
      title: 'In Progress',
      engagements: [
        { id: 3, name: 'Cloud Modernization', client: 'Initech', consultant: 'Alex Chen' },
        { id: 4, name: 'Security Audit', client: 'Umbrella Inc', consultant: 'Priya Nair' },
        { id: 5, name: 'API Integration', client: 'Soylent Co', consultant: 'Jordan Blake' },
      ],
    },
    {
      status: 'on-hold',
      title: 'On Hold',
      engagements: [
        { id: 6, name: 'Legacy Decommission', client: 'Hooli', consultant: 'Taylor Kim' },
      ],
    },
    {
      status: 'completed',
      title: 'Completed',
      engagements: [
        { id: 7, name: 'Payroll Upgrade', client: 'Wayne Enterprises', consultant: 'Morgan Reyes' },
        { id: 8, name: 'Onboarding Portal', client: 'Stark Industries', consultant: 'Casey Nguyen' },
      ],
    },
  ];

  protected connectedLists(): string[] {
    return this.columns.map((c) => c.status);
  }

  protected drop(event: CdkDragDrop<Engagement[]>): void {
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

  protected selectEngagement(engagement: Engagement): void {
    this.selected.set(engagement);
  }

  protected closeDetail(): void {
    this.selected.set(null);
  }
}
