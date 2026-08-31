import { Component, computed, input, output, signal } from '@angular/core';

import { Client, RelationshipStatus } from '../../../../types/client.types';
import { ClientCard } from '../client-card/client-card';

type StatusFilter = 'ALL' | RelationshipStatus;
type SortBy = 'companyName' | 'industry' | 'primaryContactName' | 'relationshipStatus';

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

  readonly tabs = TABS;
  readonly activeFilter = signal<StatusFilter>('ALL');

  // --- Filters (client-side only) ---
  readonly hiddenIndustries = signal<Set<string>>(new Set());
  readonly filtersModalOpen = signal(false);

  // --- Sorting (client-side only) ---
  readonly sortBy = signal<SortBy>('companyName');

  setSortBy(value: string): void {
    this.sortBy.set(value as SortBy);
  }

  readonly industries = computed<string[]>(() =>
    Array.from(new Set(this.clients().map((c) => c.industry).filter(Boolean))).sort((a, b) => a.localeCompare(b)),
  );

  readonly activeFilterCount = computed<number>(() => this.hiddenIndustries().size);

  toggleIndustryFilter(industry: string): void {
    const next = new Set(this.hiddenIndustries());
    if (next.has(industry)) {
      next.delete(industry);
    } else {
      next.add(industry);
    }
    this.hiddenIndustries.set(next);
  }

  isIndustryHidden(industry: string): boolean {
    return this.hiddenIndustries().has(industry);
  }

  showAllIndustries(): void {
    this.hiddenIndustries.set(new Set());
  }

  hideAllIndustries(): void {
    this.hiddenIndustries.set(new Set(this.industries()));
  }

  resetAllFilters(): void {
    this.hiddenIndustries.set(new Set());
  }

  readonly filteredClients = computed(() => {
    const statusFilter = this.activeFilter();
    const hiddenIndustries = this.hiddenIndustries();
    const sortBy = this.sortBy();

    return this.clients()
      .filter((c) => statusFilter === 'ALL' || c.relationshipStatus === statusFilter)
      .filter((c) => !hiddenIndustries.has(c.industry))
      .sort((a, b) => this.compareClients(a, b, sortBy));
  });

  private compareClients(a: Client, b: Client, sortBy: SortBy): number {
    switch (sortBy) {
      case 'companyName':
        return a.companyName.localeCompare(b.companyName);
      case 'industry':
        return a.industry.localeCompare(b.industry);
      case 'primaryContactName':
        return a.primaryContactName.localeCompare(b.primaryContactName);
      case 'relationshipStatus':
        return a.relationshipStatus.localeCompare(b.relationshipStatus);
    }
  }
}
