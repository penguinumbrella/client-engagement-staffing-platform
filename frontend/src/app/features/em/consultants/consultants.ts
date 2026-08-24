import { Component, OnInit, inject, signal } from '@angular/core';
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
      .getAll()
      .pipe(
        switchMap((consultants) => {
          if (consultants.length === 0) return of([]);

          const withCounts = consultants.map((consultant) =>
            this.assignmentService.getByConsultant(consultant.id).pipe(
              map((assignments): ConsultantRow => ({ ...consultant, engagementCount: assignments.length })),
              catchError(() => of<ConsultantRow>({ ...consultant, engagementCount: 0 })),
            ),
          );

          return forkJoin(withCounts);
        }),
      )
      .subscribe({
        next: (rows) => {
          this.consultants.set(rows);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load consultants.');
          this.loading.set(false);
        },
      });
  }
}
