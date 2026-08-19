import { Component, computed, input } from '@angular/core';
import { Engagement, EngagementStatus } from '../../../../types/engagement.types';

@Component({
  selector: 'app-client-engagement-card',
  imports: [],
  templateUrl: './client-engagement-card.html',
  styleUrl: './client-engagement-card.css',
})
export class ClientEngagementCard {
  engagement = input.required<Engagement>();

  readonly statusClasses = computed(() => {
    switch (this.engagement().status) {
      case EngagementStatus.IN_PROGRESS:
        return 'bg-indigo-100 text-indigo-700';
      case EngagementStatus.PLANNED:
        return 'bg-blue-100 text-blue-700';
      case EngagementStatus.ON_HOLD:
        return 'bg-amber-100 text-amber-700';
      case EngagementStatus.COMPLETED:
        return 'bg-green-100 text-green-700';
      default:
        return 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]';
    }
  });

  protected formatDate(value: string): string {
    const [year, month, day] = value.split('-').map(Number);
    return `${month}/${day}/${year}`;
  }
}
