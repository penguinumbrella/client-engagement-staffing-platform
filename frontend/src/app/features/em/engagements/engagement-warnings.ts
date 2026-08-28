import { EngagementStatus } from '../../../types/engagement.types';

export interface EngagementWarningInput {
  status: EngagementStatus;
  startDate: string;
  targetEndDate: string;
  consultantCount: number;
}

/**
 * Flags cases where an engagement's status doesn't line up with reality — either
 * today's date or its staffing — so a warning icon can surface them without the
 * user having to notice the mismatch themselves. Client-side only: computed from
 * data already loaded, nothing persisted or sent to the backend.
 */
export function engagementWarnings(input: EngagementWarningInput, today: Date = new Date()): string[] {
  const warnings: string[] = [];
  const start = new Date(input.startDate);
  const end = new Date(input.targetEndDate);
  const todayOnly = new Date(today.toISOString().slice(0, 10));

  if (input.status === EngagementStatus.IN_PROGRESS && start > todayOnly) {
    warnings.push("Marked In Progress, but its start date hasn't arrived yet.");
  }

  if (input.status === EngagementStatus.IN_PROGRESS && end < todayOnly) {
    warnings.push('Marked In Progress, but its target end date has already passed.');
  }

  if (input.status === EngagementStatus.PLANNED && start < todayOnly) {
    warnings.push('Still marked Planned, but its start date has already passed.');
  }

  if (
    (input.status === EngagementStatus.IN_PROGRESS || input.status === EngagementStatus.PLANNED) &&
    input.consultantCount === 0
  ) {
    warnings.push('No consultants are staffed on this engagement.');
  }

  return warnings;
}

export function hasEngagementWarning(input: EngagementWarningInput, today?: Date): boolean {
  return engagementWarnings(input, today).length > 0;
}
