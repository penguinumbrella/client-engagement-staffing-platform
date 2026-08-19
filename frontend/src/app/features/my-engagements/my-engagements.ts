import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
//import { CurrentConsultantService } from '../../services/current-consultant.service';
import { AssignmentService } from '../../services/assignment.service';
import { EngagementService } from '../../services/engagement.service';
import { ConsultantService } from '../../services/consultant.service';
import { ClientService } from '../../services/ClientService';
import { Engagement, EngagementStatus } from '../../types/engagement.types';
import { Consultant } from '../../types/consultant.types';
import { Assignment } from '../../types/assignment.types';
import { Client } from '../../types/client.types';
import { ClientBadge, ConsultantBadge } from '../em/engagements/engagement-detail/engagement.model';
import { engagementStatusIcon, engagementStatusIconColor } from '../em/engagements/engagement-status-icon';
import { initialsOf, colorOf } from '../../shared/avatar';
import { MyEngagementRow } from './my-engagement.model';

@Component({
  selector: 'app-my-engagements',
  imports: [FormsModule],
  templateUrl: './my-engagements.html',
  styleUrl: './my-engagements.css',
})
export class MyEngagements {
  //protected readonly currentConsultant = inject(CurrentConsultantService);
  private readonly assignmentService = inject(AssignmentService);
  private readonly engagementService = inject(EngagementService);
  private readonly consultantService = inject(ConsultantService);
  private readonly clientService = inject(ClientService);

  private readonly consultantsById = signal<Map<number, Consultant>>(new Map());
  private readonly clientsById = signal<Map<number, Client>>(new Map());
  private readonly myAssignments = signal<Assignment[]>([]);
  private readonly engagementsById = signal<Map<number, Engagement>>(new Map());
  private readonly teammatesByEngagement = signal<Map<number, ConsultantBadge[]>>(new Map());

  protected readonly expandedIds = signal<Set<number>>(new Set());
  protected readonly loading = signal(false);

  constructor() {
    this.consultantService.getAll().subscribe({
      next: (consultants) => this.consultantsById.set(new Map(consultants.map((c) => [c.id, c]))),
      error: (err) => console.error('Failed to load consultants', err),
    });

    this.clientService.getAllClients(0, 100).subscribe({
      next: (page) => this.clientsById.set(new Map(page.content.map((c) => [c.id!, c]))),
      error: (err) => console.error('Failed to load clients', err),
    });

    // effect(() => {
    //   const consultantId = this.currentConsultant.currentId();
    //   if (consultantId !== null) {
    //     this.load(consultantId);
    //   }
    // });
    this.load();
  }

  protected readonly rows = computed<MyEngagementRow[]>(() => {
    const engagementsById = this.engagementsById();
    const teammatesByEngagement = this.teammatesByEngagement();
    const clientsById = this.clientsById();

    return this.myAssignments()
      .map((assignment): MyEngagementRow | null => {
        const engagement = engagementsById.get(assignment.engagementId);
        if (!engagement) {
          return null;
        }

        const teammates = (teammatesByEngagement.get(assignment.engagementId) ?? []).filter(
          (t) => t.name !== assignment.consultantName,
        );

        const companyName = clientsById.get(engagement.clientId)?.companyName ?? 'Unknown Client';
        const client: ClientBadge = {
          companyName,
          initials: initialsOf(companyName),
          color: colorOf(companyName),
        };

        return { ...engagement, myRole: assignment.engagementRole, teammates, client };
      })
      .filter((row): row is MyEngagementRow => row !== null);
  });

  private load(): void {

    this.loading.set(true);

    this.assignmentService
      .getMine()
      .subscribe({

        next: assignments => {

          this.myAssignments.set(
            assignments
          );

          this.loading.set(false);


          assignments.forEach(
            assignment => {

              this.loadEngagement(
                assignment.engagementId
              );

              this.loadTeammates(
                assignment.engagementId
              );
            }
          );
        },

        error: err => {

          console.error(
            'Failed to load my assignments',
            err
          );

          this.loading.set(false);
        }

      });
  }

  //commented for debuging
  // private load(consultantId: number): void {
  //   this.loading.set(true);

  //   this.assignmentService.getByConsultant(consultantId).subscribe({
  //     next: (assignments) => {
  //       this.myAssignments.set(assignments);
  //       this.loading.set(false);
  //       assignments.forEach((assignment) => {
  //         this.loadEngagement(assignment.engagementId);
  //         this.loadTeammates(assignment.engagementId);
  //       });
  //     },
  //     error: (err) => {
  //       console.error(`Failed to load assignments for consultant ${consultantId}`, err);
  //       this.loading.set(false);
  //     },
  //   });
  // }

  private loadEngagement(engagementId: number): void {
    if (this.engagementsById().has(engagementId)) {
      return;
    }

    this.engagementService.getById(engagementId).subscribe({
      next: (engagement) => {
        const next = new Map(this.engagementsById());
        next.set(engagementId, engagement);
        this.engagementsById.set(next);
      },
      error: (err) => console.error(`Failed to load engagement ${engagementId}`, err),
    });
  }

  private loadTeammates(engagementId: number): void {
    this.assignmentService.getMyEngagementTeam(engagementId).subscribe({
      next: (assignments) => {
        const consultantsById = this.consultantsById();
        const badges: ConsultantBadge[] = assignments.map((a) => ({
          name: a.consultantName,
          titleRole: consultantsById.get(a.consultantId)?.titleRole ?? '',
          initials: initialsOf(a.consultantName),
          color: colorOf(a.consultantName),
          projectRole: a.engagementRole,
        }));

        const next = new Map(this.teammatesByEngagement());
        next.set(engagementId, badges);
        this.teammatesByEngagement.set(next);
      },
      error: (err) => console.error(`Failed to load teammates for engagement ${engagementId}`, err),
    });
  }

  protected toggleExpanded(engagementId: number): void {
    const next = new Set(this.expandedIds());
    if (next.has(engagementId)) {
      next.delete(engagementId);
    } else {
      next.add(engagementId);
    }
    this.expandedIds.set(next);
  }

  protected isExpanded(engagementId: number): boolean {
    return this.expandedIds().has(engagementId);
  }

  protected formatDate(date: string): string {
    return new Date(date).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  /** Matches the status badge palette used on the EM consultant-detail page. */
  private static readonly STATUS_CLASSES: Record<EngagementStatus, string> = {
    [EngagementStatus.PLANNED]: 'bg-amber-100 text-amber-700',
    [EngagementStatus.IN_PROGRESS]: 'bg-green-100 text-green-700',
    [EngagementStatus.ON_HOLD]: 'bg-orange-100 text-orange-700',
    [EngagementStatus.COMPLETED]: 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]',
    [EngagementStatus.CANCELLED]: 'bg-red-100 text-red-700',
  };

  protected statusClasses(status: EngagementStatus): string {
    return MyEngagements.STATUS_CLASSES[status] ?? 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)]';
  }

  protected readonly statusIcon = engagementStatusIcon;
  protected readonly statusIconColor = engagementStatusIconColor;

  /** Matches the progress-fill palette used on the EM consultant-detail page. */
  private static readonly PROGRESS_FILL_CLASSES: Record<EngagementStatus, string> = {
    [EngagementStatus.PLANNED]: 'bg-[var(--p-surface-500)]',
    [EngagementStatus.IN_PROGRESS]: 'bg-blue-500',
    [EngagementStatus.ON_HOLD]: 'bg-orange-400',
    [EngagementStatus.COMPLETED]: 'bg-emerald-500',
    [EngagementStatus.CANCELLED]: 'bg-[var(--p-surface-400)]',
  };

  protected progressFillClasses(status: EngagementStatus): string {
    return MyEngagements.PROGRESS_FILL_CLASSES[status] ?? 'bg-[var(--p-surface-500)]';
  }

  /** % of the way from startDate to targetEndDate, clamped 0-100. Completed/cancelled engagements read as fully done. */
  protected progressPercent(row: MyEngagementRow): number {
    if (row.status === EngagementStatus.COMPLETED || row.status === EngagementStatus.CANCELLED) {
      return 100;
    }

    const start = new Date(row.startDate).getTime();
    const end = new Date(row.targetEndDate).getTime();
    if (end <= start) {
      return 0;
    }

    const now = Date.now();
    const percent = ((now - start) / (end - start)) * 100;
    return Math.min(100, Math.max(0, Math.round(percent)));
  }
}
