import { Component, OnInit, inject, signal } from '@angular/core';

import { ClientService } from '../../../services/ClientService';
import { Client } from '../../../types/client.types';
import { ClientForm, ClientFormMode } from './client-form/client-form';
import { ClientTable } from './client-table/client-table';

@Component({
  selector: 'app-clients',
  imports: [ClientTable, ClientForm],
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

  ngOnInit(): void {
    this.loadClients();
  }

  openCreate(): void {
    this.formMode.set('create');
    this.selectedClient.set(null);
    this.formVisible.set(true);
  }

  openEdit(id: number): void {
    this.openForm('update', id);
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
