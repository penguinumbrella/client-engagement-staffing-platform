import { Component, computed, input, output, signal } from '@angular/core';

import { Client, RelationshipStatus } from '../../../../types/client.types';
import { ClientCard } from '../client-card/client-card';

type StatusFilter = 'ALL' | RelationshipStatus;

interface FilterTab {
  label: string;
  value: StatusFilter;
}

const TABS: FilterTab[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Prospective', value: RelationshipStatus.PROSPECTIVE },
  { label: 'Former', value: RelationshipStatus.FORMER },
  { label: 'Active', value: RelationshipStatus.ACTIVE },
];

@Component({
  selector: 'app-client-table',
  imports: [ClientCard],
  templateUrl: './client-table.html',
  styleUrl: './client-table.css',
})
export class ClientTable {
  clients = input<Client[]>([]);

  select = output<Client>();
  edit = output<number>();
  delete = output<number>();

  readonly tabs = TABS;
  readonly activeFilter = signal<StatusFilter>('ALL');

  readonly filteredClients = computed(() => {
    const filter = this.activeFilter();
    return filter === 'ALL' ? this.clients() : this.clients().filter((c) => c.relationshipStatus === filter);
  });
}
