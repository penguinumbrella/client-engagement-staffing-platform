import { Component, OnInit, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AssignmentService } from '../../../services/assignment.service';
import { ConsultantService } from '../../../services/consultant.service';
import { colorOf, initialsOf } from '../../../shared/avatar';
import { Consultant } from '../../../types/consultant.types';
import { ConsultantDetail } from './consultant-detail/consultant-detail';
import { ConsultantForm } from './consultant-form/consultant-form';

export interface ConsultantRow extends Consultant {
  engagementCount: number;
}

@Component({
  selector: 'app-consultants',
  imports: [ConsultantDetail, ConsultantForm],
  templateUrl: './consultants.html',
  styleUrl: './consultants.css',
})
export class Consultants implements OnInit {
  private readonly consultantService = inject(ConsultantService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly messageService = inject(MessageService);
  private readonly route = inject(ActivatedRoute);

  readonly consultants = signal<ConsultantRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly detailVisible = signal(false);
  readonly selectedConsultant = signal<Consultant | null>(null);

  readonly createFormVisible = signal(false);

  readonly initialsOf = initialsOf;
  readonly colorOf = colorOf;

  ngOnInit(): void {
    this.loadConsultants();

    // Re-run whenever the search bar navigates here with a new ?openId=,
    // even if we're already sitting on this route (component isn't re-created).
    this.route.queryParamMap.subscribe(() => this.openDetailFromQueryParam());
  }

  openDetail(consultant: Consultant): void {
    this.selectedConsultant.set(consultant);
    this.detailVisible.set(true);
  }

  openCreate(): void {
    this.createFormVisible.set(true);
  }

  onConsultantCreated(): void {
    this.loadConsultants();
  }

  private loadConsultants(): void {
    this.loading.set(true);
    this.error.set(null);

    this.consultantService
      .getAllResponse()
      .pipe(
        switchMap((response) => {
          const consultants = response.body ?? [];
          const stale = response.headers.get('X-Cache-Status') === 'stale';

          if (consultants.length === 0) return of({ rows: [] as ConsultantRow[], stale });

          const withCounts = consultants.map((consultant) =>
            this.assignmentService.getByConsultant(consultant.id).pipe(
              map((assignments): ConsultantRow => ({ ...consultant, engagementCount: assignments.length })),
              catchError(() => of<ConsultantRow>({ ...consultant, engagementCount: 0 })),
            ),
          );

          return forkJoin(withCounts).pipe(map((rows) => ({ rows, stale })));
        }),
      )
      .subscribe({
        next: ({ rows, stale }) => {
          this.consultants.set(rows);
          this.loading.set(false);
          if (stale) {
            this.messageService.add({
              severity: 'error',
              summary: 'Service Unavailable',
              detail: 'The staffing service is currently unavailable. Please try again later.',
            });
          }
          this.openDetailFromQueryParam();
        },
        error: (err) => {
          if (err.status === 503) {
            const detail = err?.error?.message ?? 'The staffing service is currently unavailable. Please try again later.';
            this.error.set(detail);
            this.messageService.add({
              severity: 'error',
              summary: 'Service Unavailable',
              detail,
            });
          } else {
            this.error.set('Failed to load consultants.');
          }
          this.loading.set(false);
        },
      });
  }

  private openDetailFromQueryParam(): void {
    const openId = Number(this.route.snapshot.queryParamMap.get('openId'));
    if (!openId) return;

    const consultant = this.consultants().find((c) => c.id === openId);
    if (consultant) this.openDetail(consultant);
  }
}
