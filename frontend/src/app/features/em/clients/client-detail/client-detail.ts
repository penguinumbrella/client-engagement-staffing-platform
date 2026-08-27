import { Component, effect, inject, input, model, output, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { EditableCompanyName } from '../editors/editable-company-name/editable-company-name';
import { EditableIndustry } from '../editors/editable-industry/editable-industry';
import { EditablePrimaryContactEmail } from '../editors/editable-primary-contact-email/editable-primary-contact-email';
import { EditablePrimaryContactName } from '../editors/editable-primary-contact-name/editable-primary-contact-name';
import { EditableRelationshipStatus } from '../editors/editable-relationship-status/editable-relationship-status';
import { ClientEngagementTable } from '../client-engagement-table/client-engagement-table';
import { Client, RelationshipStatus } from '../../../../types/client.types';
import { Engagement } from '../../../../types/engagement.types';
import { EngagementService } from '../../../../services/engagement.service';

@Component({
  selector: 'app-client-detail',
  imports: [
    EditableCompanyName,
    EditableRelationshipStatus,
    EditableIndustry,
    EditablePrimaryContactName,
    EditablePrimaryContactEmail,
    ClientEngagementTable,
  ],
  templateUrl: './client-detail.html',
  styleUrl: './client-detail.css',
})
export class ClientDetail {
  private readonly engagementService = inject(EngagementService);
  private readonly messageService = inject(MessageService);

  readonly client = input<Client | null>(null);
  readonly visible = model<boolean>(false);

  updateCompanyName = output<string>();
  updateRelationshipStatus = output<RelationshipStatus>();
  updateIndustry = output<string>();
  updatePrimaryContactName = output<string>();
  updatePrimaryContactEmail = output<string>();

  protected readonly engagements = signal<Engagement[]>([]);
  protected readonly loadingEngagements = signal(false);
  protected readonly everOpened = signal(false);

  /**
   * Mirrors `visible()`, except on the very first open it's held at `false`
   * for one extra frame after the panel is mounted (`everOpened` flips true)
   * so the browser paints the off-screen state before we transition it
   * on-screen — otherwise the slide-in CSS transition has no prior state
   * to animate from and the panel just appears.
   */
  protected readonly panelVisible = signal(false);

  constructor() {
    effect(() => {
      const client = this.client();

      if (this.visible() && client?.id != null) {
        this.loadEngagements(client.id);

        if (!this.everOpened()) {
          this.everOpened.set(true);
          requestAnimationFrame(() => this.panelVisible.set(true));
        } else {
          this.panelVisible.set(true);
        }
      } else {
        this.panelVisible.set(false);
      }
    });
  }

  protected close(): void {
    this.visible.set(false);
  }

  private loadEngagements(clientId: number): void {
    this.loadingEngagements.set(true);
    this.engagementService.getByClient(clientId).subscribe({
      next: (engagements) => {
        this.engagements.set(engagements);
        this.loadingEngagements.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load engagements for this client.',
        });
        console.error(`Failed to load engagements for client ${clientId}`, err);
        this.loadingEngagements.set(false);
      },
    });
  }
}
