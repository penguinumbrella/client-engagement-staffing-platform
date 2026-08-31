import { HttpResponse } from '@angular/common/http';
import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Engagement, EngagementStatus, EngagementType } from '../../../../types/engagement.types';
import { Assignment } from '../../../../types/assignment.types';
import { EngagementService } from '../../../../services/engagement.service';
import { AssignmentService } from '../../../../services/assignment.service';
import { ClientService } from '../../../../services/ClientService';
import { ConsultantService } from '../../../../services/consultant.service';
import { Client } from '../../../../types/client.types';
import { Consultant } from '../../../../types/consultant.types';
import { initialsOf, colorOf } from '../../../../shared/avatar';
import { engagementStatusColor } from '../engagement-status-icon';
import { engagementWarnings } from '../engagement-warnings';
import { EngagementWarningIcon } from '../engagement-warning-icon/engagement-warning-icon';
import { EngagementDetail } from '../engagement-detail/engagement-detail';
import { EngagementCard, ConsultantBadge } from '../engagement-detail/engagement.model';

const DAY_MS = 1000 * 60 * 60 * 24;
const MIN_PX_PER_DAY = 3;
const MAX_PX_PER_DAY = 24;
const DEFAULT_PX_PER_DAY = 4;
const ZOOM_FACTOR = 1.5;
/** Trailing space reserved past the last date so its month-marker/today label has room to render instead of clipping against the track edge. */
const TRAILING_LABEL_PX = 64;

interface Bar {
  leftPx: number;
  widthPx: number;
}

interface ConsultantTimelineRow extends Bar {
  consultantName: string;
  engagementRole: string;
  initials: string;
  color: string;
  startDate: string;
  endDate: string | null;
}

interface EngagementTimelineRow extends Bar {
  engagement: Engagement;
  clientName: string;
}

interface MonthMarker {
  label: string;
  leftPx: number;
}

type SortBy = 'startDate' | 'company';

/** Statuses hidden by default — an "ongoing engagements" view shouldn't open buried in finished work. */
const DEFAULT_HIDDEN_STATUSES = new Set<EngagementStatus>([EngagementStatus.COMPLETED, EngagementStatus.CANCELLED]);

@Component({
  selector: 'app-engagement-timeline',
  imports: [EngagementDetail, EngagementWarningIcon],
  templateUrl: './engagement-timeline.html',
  styleUrl: './engagement-timeline.css',
})
export class EngagementTimeline {
  private readonly engagementService = inject(EngagementService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly clientService = inject(ClientService);
  private readonly consultantService = inject(ConsultantService);
  private readonly messageService = inject(MessageService);

  private readonly engagements = signal<Engagement[]>([]);
  private readonly clientsById = signal<Map<number, Client>>(new Map());
  private readonly consultantsById = signal<Map<number, Consultant>>(new Map());
  private readonly assignmentsByEngagement = signal<Map<number, Assignment[]>>(new Map());
  private readonly consultantRowsByEngagement = signal<Map<number, ConsultantTimelineRow[]>>(new Map());

  protected readonly expanded = signal<Set<number>>(new Set());
  protected readonly loadingConsultants = signal<Set<number>>(new Set());
  protected readonly pxPerDay = signal(DEFAULT_PX_PER_DAY);
  protected readonly hiddenStatuses = signal<Set<EngagementStatus>>(new Set(DEFAULT_HIDDEN_STATUSES));
  protected readonly allStatuses = Object.values(EngagementStatus);
  protected readonly hiddenClientIds = signal<Set<number>>(new Set());
  protected readonly companyFilterOpen = signal(false);
  protected readonly sortBy = signal<SortBy>('startDate');

  protected readonly selected = signal<EngagementCard | null>(null);

  private readonly scrollContainer = viewChild<ElementRef<HTMLDivElement>>('scrollContainer');

  constructor() {
    this.clientService.getAllClientsResponse(0, 100).subscribe({
      next: (response) => {
        this.clientsById.set(new Map((response.body?.content ?? []).map((c) => [c.id!, c])));
        this.notifyIfStale(response, 'client');
      },
      error: (err) => this.notifyError('Failed to load clients.', err, 'client'),
    });

    this.consultantService.getAllResponse().subscribe({
      next: (response) => {
        this.consultantsById.set(new Map((response.body ?? []).map((c) => [c.id, c])));
        this.notifyIfStale(response, 'staffing');
      },
      error: (err) => this.notifyError('Failed to load consultants.', err, 'staffing'),
    });

    this.engagementService.getAllResponse().subscribe({
      next: (response) => {
        const engagements = response.body ?? [];
        this.engagements.set(engagements);
        engagements.forEach((e) => this.loadConsultants(e.id));
        this.notifyIfStale(response, 'engagement');
        setTimeout(() => this.jumpToToday());
      },
      error: (err) => this.notifyError('Failed to load engagements.', err, 'engagement'),
    });
  }

  protected isStatusHidden(status: EngagementStatus): boolean {
    return this.hiddenStatuses().has(status);
  }

  protected toggleStatusFilter(status: EngagementStatus): void {
    const next = new Set(this.hiddenStatuses());
    if (next.has(status)) {
      next.delete(status);
    } else {
      next.add(status);
    }
    this.hiddenStatuses.set(next);
  }

  protected readonly clientOptions = computed<Client[]>(() =>
    Array.from(this.clientsById().values()).sort((a, b) => a.companyName.localeCompare(b.companyName)),
  );

  protected isClientHidden(clientId: number): boolean {
    return this.hiddenClientIds().has(clientId);
  }

  protected toggleClientFilter(clientId: number): void {
    const next = new Set(this.hiddenClientIds());
    if (next.has(clientId)) {
      next.delete(clientId);
    } else {
      next.add(clientId);
    }
    this.hiddenClientIds.set(next);
  }

  protected showAllClients(): void {
    this.hiddenClientIds.set(new Set());
  }

  protected hideAllClients(): void {
    this.hiddenClientIds.set(new Set(this.clientOptions().map((c) => c.id!)));
  }

  protected toggleCompanyFilterOpen(): void {
    this.companyFilterOpen.set(!this.companyFilterOpen());
  }

  protected setSortBy(sortBy: SortBy): void {
    this.sortBy.set(sortBy);
  }

  private readonly visibleEngagements = computed<Engagement[]>(() => {
    const hiddenStatuses = this.hiddenStatuses();
    const hiddenClientIds = this.hiddenClientIds();
    return this.engagements().filter((e) => !hiddenStatuses.has(e.status) && !hiddenClientIds.has(e.clientId));
  });

  /** Shared date range every bar (engagement and consultant) is positioned against, so they all line up on one axis. */
  private readonly range = computed<{ start: number; end: number }>(() => {
    const engagements = this.visibleEngagements();
    if (!engagements.length) {
      const now = Date.parse(new Date().toISOString());
      return { start: now, end: now + 1 };
    }

    const starts = engagements.map((e) => new Date(e.startDate).getTime());
    const ends = engagements.map((e) => new Date(e.targetEndDate).getTime());
    const min = Math.min(...starts);
    const max = Math.max(...ends);
    const pad = Math.max((max - min) * 0.02, DAY_MS);
    return { start: min - pad, end: max + pad };
  });

  protected readonly trackWidthPx = computed<number>(() => {
    const { start, end } = this.range();
    return Math.max(((end - start) / DAY_MS) * this.pxPerDay(), 1) + TRAILING_LABEL_PX;
  });

  private toBar(startDate: string, endDate: string | null): Bar {
    const { start, end } = this.range();
    const pxPerDay = this.pxPerDay();
    const s = new Date(startDate).getTime();
    const e = endDate ? new Date(endDate).getTime() : end;
    const leftPx = ((s - start) / DAY_MS) * pxPerDay;
    const widthPx = Math.max(((e - s) / DAY_MS) * pxPerDay, 3);
    return { leftPx, widthPx };
  }

  protected zoomIn(): void {
    this.pxPerDay.set(Math.min(this.pxPerDay() * ZOOM_FACTOR, MAX_PX_PER_DAY));
  }

  protected zoomOut(): void {
    this.pxPerDay.set(Math.max(this.pxPerDay() / ZOOM_FACTOR, MIN_PX_PER_DAY));
  }

  /** Scrolls the track so "today" is centered in the visible viewport — the quickest way back to the current timeline after panning/zooming around. */
  protected jumpToToday(): void {
    const todayPx = this.todayLinePx();
    const el = this.scrollContainer()?.nativeElement;
    if (todayPx === null || !el) {
      return;
    }
    el.scrollTo({ left: Math.max(todayPx - el.clientWidth / 2, 0), behavior: 'smooth' });
  }

  protected readonly statusBarColor = engagementStatusColor;

  /** Pixel offset of "now" within the track, relative to the label column, or null if today falls outside the loaded date range. */
  protected readonly todayLinePx = computed<number | null>(() => {
    const { start, end } = this.range();
    const now = Date.now();
    if (now < start || now > end) {
      return null;
    }
    return ((now - start) / DAY_MS) * this.pxPerDay();
  });

  protected readonly todayLabel = new Date().toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });

  protected readonly rows = computed<EngagementTimelineRow[]>(() => {
    const clientsById = this.clientsById();
    const nameOf = (e: Engagement) => clientsById.get(e.clientId)?.companyName ?? 'Unknown Client';
    const sortBy = this.sortBy();

    return this.visibleEngagements()
      .slice()
      .sort((a, b) =>
        sortBy === 'company'
          ? nameOf(a).localeCompare(nameOf(b)) || new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
          : new Date(a.startDate).getTime() - new Date(b.startDate).getTime(),
      )
      .map((engagement) => ({
        engagement,
        clientName: nameOf(engagement),
        ...this.toBar(engagement.startDate, engagement.targetEndDate),
      }));
  });

  protected readonly monthMarkers = computed<MonthMarker[]>(() => {
    const { start, end } = this.range();
    const pxPerDay = this.pxPerDay();
    const markers: MonthMarker[] = [];

    const cursor = new Date(start);
    cursor.setDate(1);
    cursor.setHours(0, 0, 0, 0);
    if (cursor.getTime() < start) {
      cursor.setMonth(cursor.getMonth() + 1);
    }

    while (cursor.getTime() <= end) {
      markers.push({
        label: cursor.toLocaleDateString(undefined, { month: 'short', year: 'numeric' }),
        leftPx: ((cursor.getTime() - start) / DAY_MS) * pxPerDay,
      });
      cursor.setMonth(cursor.getMonth() + 1);
    }

    return markers;
  });

  protected consultantRows(engagementId: number): ConsultantTimelineRow[] {
    return this.consultantRowsByEngagement().get(engagementId) ?? [];
  }

  protected rowWarnings(engagement: Engagement): string[] {
    return engagementWarnings({
      status: engagement.status,
      startDate: engagement.startDate,
      targetEndDate: engagement.targetEndDate,
      consultantCount: this.consultantRows(engagement.id).length,
    });
  }

  protected isExpanded(engagementId: number): boolean {
    return this.expanded().has(engagementId);
  }

  protected toggleExpanded(engagementId: number): void {
    const next = new Set(this.expanded());
    if (next.has(engagementId)) {
      next.delete(engagementId);
    } else {
      next.add(engagementId);
    }
    this.expanded.set(next);
  }

  protected loadConsultants(engagementId: number): void {
    const loading = new Set(this.loadingConsultants());
    loading.add(engagementId);
    this.loadingConsultants.set(loading);

    this.assignmentService.getByEngagement(engagementId).subscribe({
      next: (assignments) => {
        const nextAssignments = new Map(this.assignmentsByEngagement());
        nextAssignments.set(engagementId, assignments);
        this.assignmentsByEngagement.set(nextAssignments);

        const rows = assignments.map((a) => ({
          consultantName: a.consultantName,
          engagementRole: a.engagementRole,
          initials: initialsOf(a.consultantName),
          color: colorOf(a.consultantName),
          startDate: a.assignmentStartDate,
          endDate: a.assignmentEndDate,
          ...this.toBar(a.assignmentStartDate, a.assignmentEndDate),
        }));

        const nextRows = new Map(this.consultantRowsByEngagement());
        nextRows.set(engagementId, rows);
        this.consultantRowsByEngagement.set(nextRows);
        this.stopLoading(engagementId);
      },
      error: (err) => {
        this.notifyError('Failed to load consultants for an engagement.', err, 'staffing');
        this.stopLoading(engagementId);
      },
    });
  }

  protected openDetail(engagement: Engagement): void {
    this.selected.set(this.toCard(engagement));
  }

  protected closeDetail(): void {
    this.selected.set(null);
  }

  private toCard(engagement: Engagement): EngagementCard {
    const companyName = this.clientsById().get(engagement.clientId)?.companyName ?? 'Unknown Client';
    const consultantsById = this.consultantsById();
    const consultants: ConsultantBadge[] = (this.assignmentsByEngagement().get(engagement.id) ?? []).map((a) => ({
      name: a.consultantName,
      titleRole: consultantsById.get(a.consultantId)?.titleRole ?? '',
      initials: initialsOf(a.consultantName),
      color: colorOf(a.consultantName),
      projectRole: a.engagementRole,
    }));

    return {
      ...engagement,
      client: { companyName, initials: initialsOf(companyName), color: colorOf(companyName) },
      consultants,
    };
  }

  private patchSelected(id: number, patch: Partial<Engagement>): void {
    const current = this.selected();
    if (current && current.id === id) {
      this.selected.set({ ...current, ...patch });
    }
  }

  protected onEngagementDeleted(id: number): void {
    this.engagements.set(this.engagements().filter((e) => e.id !== id));
    this.selected.set(null);
  }

  protected onEngagementCancelled(updated: Engagement): void {
    this.engagements.set(this.engagements().map((e) => (e.id === updated.id ? updated : e)));
    this.patchSelected(updated.id, { status: updated.status });
  }

  protected updateSummary(id: number, summary: string): void {
    this.engagementService.update(id, { summary }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));
        this.patchSelected(id, { summary: updated.summary });
      },
      error: (err) => this.notifyUpdateError('summary', id, err),
    });
  }

  protected updateName(id: number, engagementName: string): void {
    this.engagementService.update(id, { engagementName }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));
        this.patchSelected(id, { engagementName: updated.engagementName });
      },
      error: (err) => this.notifyUpdateError('name', id, err),
    });
  }

  protected updateDates(id: number, dates: { startDate: string; targetEndDate: string }): void {
    this.engagementService.update(id, dates).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));
        this.patchSelected(id, { startDate: updated.startDate, targetEndDate: updated.targetEndDate });
      },
      error: (err) => this.notifyUpdateError('dates', id, err),
    });
  }

  protected updateType(id: number, engagementType: EngagementType): void {
    this.engagementService.update(id, { engagementType }).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));
        this.patchSelected(id, { engagementType: updated.engagementType });
      },
      error: (err) => this.notifyUpdateError('type', id, err),
    });
  }

  protected updateStatus(id: number, status: EngagementStatus): void {
    this.engagementService.updateStatus(id, status).subscribe({
      next: (updated) => {
        this.engagements.set(this.engagements().map((e) => (e.id === id ? updated : e)));
        this.patchSelected(id, { status: updated.status });
      },
      error: (err) => this.notifyUpdateError('status', id, err),
    });
  }

  private notifyIfStale(response: HttpResponse<unknown>, service: string): void {
    if (response.headers.get('X-Cache-Status') !== 'stale') {
      return;
    }

    this.messageService.add({
      severity: 'error',
      summary: 'Service Unavailable',
      detail: `The ${service} service is currently unavailable. Please try again later.`,
    });
  }

  private notifyUpdateError(field: string, engagementId: number, err: unknown): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Update Failed',
      detail: (err as { error?: { message?: string } })?.error?.message ?? `Failed to update the engagement ${field}. Please try again.`,
    });
    console.error(`Failed to update engagement ${engagementId} ${field}`, err);
  }

  private stopLoading(engagementId: number): void {
    const next = new Set(this.loadingConsultants());
    next.delete(engagementId);
    this.loadingConsultants.set(next);
  }

  /**
   * `service` names which downstream service the failed call was ultimately
   * headed to, so a 503 (the whole service down, per the gateway's circuit
   * breaker) can say so specifically instead of a generic message.
   */
  private notifyError(detail: string, err: any, service?: string): void {
    const unavailable = err?.status === 503;
    this.messageService.add({
      severity: 'error',
      summary: unavailable ? 'Service Unavailable' : 'Error',
      detail: unavailable
        ? (err?.error?.message ?? `The ${service} service is currently unavailable. Please try again later.`)
        : detail,
    });
    console.error(detail, err);
  }
}
