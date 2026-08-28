import { Component, computed, input, output } from '@angular/core';
import { Client, RelationshipStatus } from '../../../../types/client.types';

@Component({
  selector: 'app-client-card',
  imports: [],
  templateUrl: './client-card.html',
  styleUrl: './client-card.css',
})
export class ClientCard {
  client = input.required<Client>();

  select = output<Client>();

  readonly statusClasses = computed(() => {
    switch (this.client().relationshipStatus) {
      case RelationshipStatus.ACTIVE:
        return 'bg-green-100 text-green-700';
      case RelationshipStatus.PROSPECTIVE:
        return 'bg-blue-100 text-blue-700';
      case RelationshipStatus.FORMER:
        return 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]';
      default:
        return 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]';
    }
  });
}
