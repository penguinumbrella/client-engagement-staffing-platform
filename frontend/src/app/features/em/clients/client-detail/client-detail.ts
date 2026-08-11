import { Component, effect, inject, input, output, signal } from '@angular/core';
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

  client = input.required<Client>();

  close = output<void>();
  updateCompanyName = output<string>();
  updateRelationshipStatus = output<RelationshipStatus>();
  updateIndustry = output<string>();
  updatePrimaryContactName = output<string>();
  updatePrimaryContactEmail = output<string>();

  protected readonly engagements = signal<Engagement[]>([]);
  protected readonly loadingEngagements = signal(true);

  constructor() {
    effect(() => {
      const id = this.client().id;
      if (id == null) {
        return;
      }

      this.loadingEngagements.set(true);
      this.engagementService.getByClient(id).subscribe({
        next: (engagements) => {
          this.engagements.set(engagements);
          this.loadingEngagements.set(false);
        },
        error: (err) => {
          console.error(`Failed to load engagements for client ${id}`, err);
          this.loadingEngagements.set(false);
        },
      });
    });
  }
}
