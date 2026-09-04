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
import { SpinnyBee } from '../../../shared/spinny-bee/spinny-bee';

export interface ConsultantRow extends Consultant {
  engagementCount: number;
}

type StaffingFilter = 'all' | 'staffed' | 'unstaffed';
type SortBy = 'name' | 'titleRole' | 'primarySkillArea' | 'engagementCount';

@Component({
  selector: 'app-consultants',
  imports: [ConsultantDetail, ConsultantForm, SpinnyBee],
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

  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly detailVisible = signal(false);
  readonly selectedConsultant = signal<Consultant | null>(null);

  readonly createFormVisible = signal(false);

  readonly initialsOf = initialsOf;
  readonly colorOf = colorOf;

  // --- Filters ---
  readonly hiddenSkillAreas = signal<Set<SkillArea>>(new Set());
  readonly staffingFilter = signal<StaffingFilter>('all');
  readonly filtersModalOpen = signal(false);

  // --- Sorting (client-side on the current page) ---
  readonly sortBy = signal<SortBy>('name');

  readonly skillAreas = Object.values(SkillArea);
  readonly pageSizeOptions = [10, 25, 50];

  readonly pageStart = computed(() =>
    this.totalElements() === 0 ? 0 : this.page() * this.pageSize() + 1,
  );

  readonly pageEnd = computed(() =>
    Math.min((this.page() + 1) * this.pageSize(), this.totalElements()),
  );

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
    this.resetToFirstPageAndReload();
  }

  isSkillAreaHidden(skillArea: SkillArea): boolean {
    return this.hiddenSkillAreas().has(skillArea);
  }

  showAllSkillAreas(): void {
    this.hiddenSkillAreas.set(new Set());
    this.resetToFirstPageAndReload();
  }

  hideAllSkillAreas(): void {
    this.hiddenSkillAreas.set(new Set(this.skillAreas));
    this.resetToFirstPageAndReload();
  }

  setStaffingFilter(value: string): void {
    this.staffingFilter.set(value as StaffingFilter);
  }

  resetAllFilters(): void {
    this.hiddenSkillAreas.set(new Set());
    this.staffingFilter.set('all');
    this.resetToFirstPageAndReload();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.loadConsultants();
  }

  setPageSize(value: string): void {
    this.pageSize.set(Number(value));
    this.resetToFirstPageAndReload();
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

  private resetToFirstPageAndReload(): void {
    this.page.set(0);
    this.loadConsultants();
  }

  private visibleSkillAreas(): SkillArea[] | undefined {
    const hidden = this.hiddenSkillAreas();
    if (hidden.size === 0) return undefined;
    return this.skillAreas.filter((skillArea) => !hidden.has(skillArea));
  }

  private loadConsultants(): void {
    const skillAreas = this.visibleSkillAreas();
    if (skillAreas && skillAreas.length === 0) {
      this.consultants.set([]);
      this.totalElements.set(0);
      this.totalPages.set(0);
      this.loading.set(false);
      this.error.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.consultantService
      .getAllResponse(this.page(), this.pageSize(), skillAreas)
      .pipe(
        switchMap((response) => {
          const page = response.body;
          const consultants = page?.content ?? [];
          const stale = response.headers.get('X-Cache-Status') === 'stale';

          this.totalElements.set(page?.totalElements ?? 0);
          this.totalPages.set(page?.totalPages ?? 0);

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
    if (!openId || this.loading()) return;

    const consultant = this.consultants().find((c) => c.id === openId);
    if (consultant) {
      this.openDetail(consultant);
      return;
    }

    this.consultantService.getById(openId).subscribe({
      next: (found) => this.openDetail(found),
      error: () => {},
    });
  }
}
