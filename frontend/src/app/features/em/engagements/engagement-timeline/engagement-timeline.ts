import { Component, computed, inject, signal } from '@angular/core';
import { Engagement, EngagementStatus } from '../../../../types/engagement.types';
import { EngagementService } from '../../../../services/engagement.service';
import { AssignmentService } from '../../../../services/assignment.service';
import { ClientService } from '../../../../services/ClientService';
import { Client } from '../../../../types/client.types';
import { initialsOf, colorOf } from '../../../../shared/avatar';
import { engagementStatusColor } from '../engagement-status-icon';

const DAY_MS = 1000 * 60 * 60 * 24;
const MIN_PX_PER_DAY = 1;
const MAX_PX_PER_DAY = 48;
const DEFAULT_PX_PER_DAY = 4;
const ZOOM_FACTOR = 1.5;

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

@Component({
  selector: 'app-engagement-timeline',
  templateUrl: './engagement-timeline.html',
  styleUrl: './engagement-timeline.css',
})
export class EngagementTimeline {
  private readonly engagementService = inject(EngagementService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly clientService = inject(ClientService);

  private readonly engagements = signal<Engagement[]>([]);
  private readonly clientsById = signal<Map<number, Client>>(new Map());
  private readonly consultantRowsByEngagement = signal<Map<number, ConsultantTimelineRow[]>>(new Map());

  protected readonly expanded = signal<Set<number>>(new Set());
  protected readonly loadingConsultants = signal<Set<number>>(new Set());
  protected readonly pxPerDay = signal(DEFAULT_PX_PER_DAY);

  constructor() {
    this.clientService.getAllClients(0, 100).subscribe({
      next: (page) => this.clientsById.set(new Map(page.content.map((c) => [c.id!, c]))),
      error: (err) => console.error('Failed to load clients', err),
    });

    this.engagementService.getAll().subscribe({
      next: (engagements) => {
        this.engagements.set(engagements);
        engagements.forEach((e) => this.loadConsultants(e.id));
      },
      error: (err) => console.error('Failed to load engagements', err),
    });
  }

  /** Shared date range every bar (engagement and consultant) is positioned against, so they all line up on one axis. */
  private readonly range = computed<{ start: number; end: number }>(() => {
    const engagements = this.engagements();
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
    return Math.max(((end - start) / DAY_MS) * this.pxPerDay(), 1);
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

  protected readonly rows = computed<EngagementTimelineRow[]>(() => {
    const clientsById = this.clientsById();
    return this.engagements()
      .slice()
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
      .map((engagement) => ({
        engagement,
        clientName: clientsById.get(engagement.clientId)?.companyName ?? 'Unknown Client',
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

  private loadConsultants(engagementId: number): void {
    const loading = new Set(this.loadingConsultants());
    loading.add(engagementId);
    this.loadingConsultants.set(loading);

    this.assignmentService.getByEngagement(engagementId).subscribe({
      next: (assignments) => {
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
        console.error(`Failed to load assignments for engagement ${engagementId}`, err);
        this.stopLoading(engagementId);
      },
    });
  }

  private stopLoading(engagementId: number): void {
    const next = new Set(this.loadingConsultants());
    next.delete(engagementId);
    this.loadingConsultants.set(next);
  }
}
