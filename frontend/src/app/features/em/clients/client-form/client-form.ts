import { Component, computed, effect, inject, input, model, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';

import { ClientService } from '../../../../services/ClientService';
import { Client, RelationshipStatus } from '../../../../types/client.types';

export type ClientFormMode = 'create' | 'update' | 'delete';

@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, ConfirmDialog],
  providers: [ConfirmationService],
  templateUrl: './client-form.html',
  styleUrl: './client-form.css',
})
export class ClientForm {
  mode = input.required<ClientFormMode>();
  client = input<Client | null>(null);
  visible = model<boolean>(false);

  saved = output<Client>();
  deleted = output<number>();
  cancel = output<void>();

  private clientService = inject(ClientService);
  private formBuilder = inject(FormBuilder);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  readonly relationshipStatuses = Object.values(RelationshipStatus);
  readonly submitting = signal(false);

  readonly isDelete = computed(() => this.mode() === 'delete');
  readonly title = computed(() => {
    switch (this.mode()) {
      case 'create':
        return 'Add Client';
      case 'update':
        return 'Edit Client';
      case 'delete':
        return 'Delete Client';
    }
  });

  form!: FormGroup;

  constructor() {
    this.form = this.formBuilder.nonNullable.group({
      companyName: ['', Validators.required],
      industry: [''],
      primaryContactName: [''],
      primaryContactEmail: ['', Validators.email],
      relationshipStatus: [RelationshipStatus.PROSPECTIVE, Validators.required],
    });

    effect(() => {
      const client = this.client();

      this.form.reset({
        companyName: client?.companyName ?? '',
        industry: client?.industry ?? '',
        primaryContactName: client?.primaryContactName ?? '',
        primaryContactEmail: client?.primaryContactEmail ?? '',
        relationshipStatus: client?.relationshipStatus ?? RelationshipStatus.PROSPECTIVE,
      });

      this.isDelete() ? this.form.disable() : this.form.enable();
    });
  }

  onSubmit(): void {
    if (this.isDelete()) {
      this.confirmDelete();
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitSave();
  }

  onCancel(): void {
    this.visible.set(false);
    this.cancel.emit();
  }

  private confirmDelete(): void {
    const client = this.client();
    if (!client) return;

    this.confirmationService.confirm({
      header: 'Delete Client',
      message: `Are you sure you want to delete ${client.companyName}? This cannot be undone.`,
      icon: 'pi pi-exclamation-triangle',
      acceptButtonProps: { label: 'Delete', severity: 'danger' },
      rejectButtonProps: { label: 'Cancel', severity: 'secondary', outlined: true },
      accept: () => this.submitDelete(),
    });
  }

  private submitDelete(): void {
    const id = this.client()?.id;
    if (id == null) return;

    this.submitting.set(true);
    this.clientService.deleteClient(id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.visible.set(false);
        this.deleted.emit(id);
      },
      error: (err) => {
        this.submitting.set(false);

        if (err.status === 409) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Cannot Delete',
            detail: 'This client has active engagements. Complete or cancel them first, then delete the client.',
          });
        } else if (err.status === 503) {
          this.messageService.add({
            severity: 'error',
            summary: 'Service Unavailable',
            detail: err?.error?.message ?? 'The client service is currently unavailable. Please try again later.',
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Delete Failed',
            detail: 'Failed to delete client. Please try again.',
          });
        }

        console.error(err);
      },
    });
  }

  private submitSave(): void {
    const value = this.form.getRawValue();
    const id = this.client()?.id;

    this.submitting.set(true);
    const request$ =
      this.mode() === 'create'
        ? this.clientService.createClient(value)
        : this.clientService.updateClient(id!, value);

    request$.subscribe({
      next: (client) => {
        this.submitting.set(false);
        this.visible.set(false);
        this.saved.emit(client);
      },
      error: (err) => {
        this.submitting.set(false);

        if (err.status === 409) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Duplicate Client',
            detail: err?.error?.message ?? 'A client with this name already exists.',
          });
        } else if (err.status === 503) {
          this.messageService.add({
            severity: 'error',
            summary: 'Service Unavailable',
            detail: err?.error?.message ?? 'The client service is currently unavailable. Please try again later.',
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: this.mode() === 'create' ? 'Create Failed' : 'Update Failed',
            detail: `Failed to ${this.mode() === 'create' ? 'create' : 'update'} client. Please try again.`,
          });
        }

        console.error(err);
      },
    });
  }
}
