import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { EngagementCard, ConsultantBadge } from '../engagement.model';
import { EngagementRole } from '../../../../types/assignment.types';

interface RoleGroup {
  role: EngagementRole;
  label: string;
  consultants: ConsultantBadge[];
}

const ROLE_LABELS: Record<EngagementRole, string> = {
  [EngagementRole.LEAD]: 'Lead',
  [EngagementRole.SENIOR_ASSOCIATE]: 'Senior Associate',
  [EngagementRole.ASSOCIATE]: 'Associate',
};

@Component({
  selector: 'app-engagement-detail',
  imports: [],
  templateUrl: './engagement-detail.html',
  styleUrl: './engagement-detail.css',
})
export class EngagementDetail implements OnInit {
  @Input({ required: true }) engagement!: EngagementCard;
  @Output() close = new EventEmitter<void>();

  protected readonly expandedRoles = signal<Set<EngagementRole>>(new Set());
  protected roleGroups: RoleGroup[] = [];

  ngOnInit(): void {
    const groups = new Map<EngagementRole, ConsultantBadge[]>();

    for (const consultant of this.engagement.consultants) {
      const group = groups.get(consultant.projectRole);
      if (group) {
        group.push(consultant);
      } else {
        groups.set(consultant.projectRole, [consultant]);
      }
    }

    this.roleGroups = Array.from(groups, ([role, consultants]) => ({
      role,
      label: ROLE_LABELS[role],
      consultants,
    }));
    this.expandedRoles.set(new Set(this.roleGroups.map((group) => group.role)));
  }

  protected formatDate(value: string): string {
    const [year, month, day] = value.split('-').map(Number);
    return `${month}/${day}/${year}`;
  }

  protected isExpanded(role: EngagementRole): boolean {
    return this.expandedRoles().has(role);
  }

  protected toggleRole(role: EngagementRole): void {
    const next = new Set(this.expandedRoles());
    if (next.has(role)) {
      next.delete(role);
    } else {
      next.add(role);
    }
    this.expandedRoles.set(next);
  }
}
