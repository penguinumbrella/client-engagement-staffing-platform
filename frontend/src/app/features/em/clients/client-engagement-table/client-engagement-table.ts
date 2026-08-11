import { Component, computed, input, signal } from '@angular/core';

import { Engagement, EngagementStatus } from '../../../../types/engagement.types';
import { ClientEngagementCard } from '../client-engagement-card/client-engagement-card';

type StatusFilter = 'ALL' | EngagementStatus;

interface FilterTab {
  label: string;
  value: StatusFilter;
}

const TABS: FilterTab[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Planned', value: EngagementStatus.PLANNED },
  { label: 'In Progress', value: EngagementStatus.IN_PROGRESS },
  { label: 'On Hold', value: EngagementStatus.ON_HOLD },
  { label: 'Completed', value: EngagementStatus.COMPLETED },
];

@Component({
  selector: 'app-client-engagement-table',
  imports: [ClientEngagementCard],
  templateUrl: './client-engagement-table.html',
  styleUrl: './client-engagement-table.css',
})
export class ClientEngagementTable {
  engagements = input<Engagement[]>([]);

  readonly tabs = TABS;
  readonly activeFilter = signal<StatusFilter>('ALL');

  readonly filteredEngagements = computed(() => {
    const filter = this.activeFilter();
    return filter === 'ALL' ? this.engagements() : this.engagements().filter((e) => e.status === filter);
  });
}
