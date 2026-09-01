import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AssignmentService } from '../../../services/assignment.service';
import { ConsultantService } from '../../../services/consultant.service';
import { colorOf, initialsOf } from '../../../shared/avatar';
import { Consultant, SkillArea } from '../../../types/consultant.types';
import { ConsultantDetail } from './consultant-detail/consultant-detail';
import { ConsultantForm } from './consultant-form/consultant-form';

export interface ConsultantRow extends Consultant {
  engagementCount: number;
}

type StaffingFilter = 'all' | 'staffed' | 'unstaffed';
type SortBy = 'name' | 'titleRole' | 'primarySkillArea' | 'engagementCount';

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

  // --- Filters (client-side only) ---
  readonly hiddenSkillAreas = signal<Set<SkillArea>>(new Set());
  readonly staffingFilter = signal<StaffingFilter>('all');
  readonly filtersModalOpen = signal(false);

  // --- Sorting (client-side only) ---
  readonly sortBy = signal<SortBy>('name');

  readonly skillAreas = Object.values(SkillArea);

  setSortBy(value: string): void {
    this.sortBy.set(value as SortBy);
  }

  readonly activeFilterCount = computed<number>(() => {
    let count = this.hiddenSkillAreas().size;
    if (this.staffingFilter() !== 'all') count++;
    return count;
  });

  toggleSkillAreaFilter(skillArea: SkillArea): void {
    const next = new Set(this.hiddenSkillAreas());
    if (next.has(skillArea)) {
      next.delete(skillArea);
    } else {
      next.add(skillArea);
    }
    this.hiddenSkillAreas.set(next);
  }

  isSkillAreaHidden(skillArea: SkillArea): boolean {
    return this.hiddenSkillAreas().has(skillArea);
  }

  showAllSkillAreas(): void {
    this.hiddenSkillAreas.set(new Set());
  }

  hideAllSkillAreas(): void {
    this.hiddenSkillAreas.set(new Set(this.skillAreas));
  }

  setStaffingFilter(value: string): void {
    this.staffingFilter.set(value as StaffingFilter);
  }

  resetAllFilters(): void {
    this.hiddenSkillAreas.set(new Set());
    this.staffingFilter.set('all');
  }

  readonly filteredConsultants = computed<ConsultantRow[]>(() => {
    const hiddenSkillAreas = this.hiddenSkillAreas();
    const staffingFilter = this.staffingFilter();
    const sortBy = this.sortBy();

    return this.consultants()
      .filter((c) => !hiddenSkillAreas.has(c.primarySkillArea))
      .filter((c) => {
        if (staffingFilter === 'staffed' && c.engagementCount === 0) return false;
        if (staffingFilter === 'unstaffed' && c.engagementCount > 0) return false;
        return true;
      })
      .sort((a, b) => this.compareConsultants(a, b, sortBy));
  });

  private compareConsultants(a: ConsultantRow, b: ConsultantRow, sortBy: SortBy): number {
    switch (sortBy) {
      case 'name':
        return a.name.localeCompare(b.name);
      case 'titleRole':
        return a.titleRole.localeCompare(b.titleRole);
      case 'primarySkillArea':
        return a.primarySkillArea.localeCompare(b.primarySkillArea);
      case 'engagementCount':
        return b.engagementCount - a.engagementCount;
    }
  }

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
