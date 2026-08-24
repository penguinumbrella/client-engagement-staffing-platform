import { Injectable, computed, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ConsultantService } from './consultant.service';
import { Consultant } from '../types/consultant.types';

/**
 * Temporary stand-in for auth on the consultant-facing side. Until an AuthService
 * resolves the signed-in user to a Consultant, this lets the "viewing as" consultant
 * be switched manually (see my-engagements.html). Remove once real auth lands.
 */
@Injectable({ providedIn: 'root' })
export class CurrentConsultantService {
  private readonly consultantService = inject(ConsultantService);
  private readonly messageService = inject(MessageService);

  private readonly consultants = signal<Consultant[]>([]);
  private readonly selectedId = signal<number | null>(null);

  constructor() {
    this.consultantService.getAll().subscribe({
      next: (consultants) => {
        this.consultants.set(consultants);
        if (this.selectedId() === null && consultants.length > 0) {
          this.selectedId.set(consultants[0].id);
        }
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load consultants.',
        });
        console.error('Failed to load consultants', err);
      },
    });
  }

  readonly all = this.consultants.asReadonly();
  readonly currentId = this.selectedId.asReadonly();
  readonly current = computed(() => this.consultants().find((c) => c.id === this.selectedId()) ?? null);

  setCurrent(consultantId: number): void {
    this.selectedId.set(consultantId);
  }
}
