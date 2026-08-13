import { Component, OnInit, inject, signal } from '@angular/core';

import { ClientService } from '../../../services/ClientService';
import { Client, RelationshipStatus } from '../../../types/client.types';
import { ClientDetail } from './client-detail/client-detail';
import { ClientForm, ClientFormMode } from './client-form/client-form';
import { ClientTable } from './client-table/client-table';

@Component({
  selector: 'app-clients',
  imports: [ClientTable, ClientForm, ClientDetail],
  templateUrl: './clients.html',
  styleUrl: './clients.css',
})
export class Clients implements OnInit {
  private readonly clientService = inject(ClientService);

  readonly clients = signal<Client[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly formVisible = signal(false);
  readonly formMode = signal<ClientFormMode>('create');
  readonly selectedClient = signal<Client | null>(null);

  readonly detailClient = signal<Client | null>(null);
  readonly detailVisible = signal(false);

  ngOnInit(): void {
    this.loadClients();
  }

  openCreate(): void {
    this.formMode.set('create');
    this.selectedClient.set(null);
    this.formVisible.set(true);
  }

  openDelete(id: number): void {
    this.openForm('delete', id);
  }

  onSaved(): void {
    this.loadClients();
  }

  onDeleted(): void {
    this.loadClients();
  }

  openDetail(client: Client): void {
    this.detailClient.set(client);
    this.detailVisible.set(true);
  }

  updateCompanyName(id: number, companyName: string): void {
    this.patchClient(id, { companyName });
  }

  updateRelationshipStatus(id: number, relationshipStatus: RelationshipStatus): void {
    this.patchClient(id, { relationshipStatus });
  }

  updateIndustry(id: number, industry: string): void {
    this.patchClient(id, { industry });
  }

  updatePrimaryContactName(id: number, primaryContactName: string): void {
    this.patchClient(id, { primaryContactName });
  }

  updatePrimaryContactEmail(id: number, primaryContactEmail: string): void {
    this.patchClient(id, { primaryContactEmail });
  }

  private patchClient(id: number, patch: Partial<Client>): void {
    const current = this.clients().find((c) => c.id === id);
    if (!current) return;

    this.clientService.updateClient(id, { ...current, ...patch }).subscribe({
      next: (updated) => {
        this.clients.set(this.clients().map((c) => (c.id === id ? updated : c)));

        const detail = this.detailClient();
        if (detail && detail.id === id) {
          this.detailClient.set(updated);
        }
      },
      error: (err) => console.error(`Failed to update client ${id}`, err),
    });
  }

  private openForm(mode: ClientFormMode, id: number): void {
    const client = this.clients().find((c) => c.id === id);
    if (!client) return;

    this.formMode.set(mode);
    this.selectedClient.set(client);
    this.formVisible.set(true);
  }

  private loadClients(): void {
    this.loading.set(true);
    this.clientService.getAllClients().subscribe({
      next: (page) => {
        this.clients.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load clients.');
        this.loading.set(false);
      },
    });
  }
}
