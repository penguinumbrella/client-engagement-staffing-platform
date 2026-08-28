import { EngagementStatus } from '../../../../types/engagement.types';

export interface AssignmentWarningInput {
  id: number;
  engagementName: string;
  status: EngagementStatus;
  startDate: string;
  endDate: string;
}

const WRAPPED_UP_STATUSES = new Set<EngagementStatus>([EngagementStatus.COMPLETED, EngagementStatus.CANCELLED]);

/**
 * Flags assignments whose dates don't line up with their engagement's status, or that
 * overlap another assignment for the same consultant. Client-side only: computed from
 * the rows already loaded for this panel, nothing persisted or sent to the backend.
 */
export function assignmentWarnings(
  rows: AssignmentWarningInput[],
  today: Date = new Date(),
): Map<number, string[]> {
  const warningsById = new Map<number, string[]>();
  const todayOnly = new Date(today.toISOString().slice(0, 10));

  const addWarning = (id: number, message: string) => {
    warningsById.set(id, [...(warningsById.get(id) ?? []), message]);
  };

  for (const row of rows) {
    const end = row.endDate ? new Date(row.endDate) : null;

    if (WRAPPED_UP_STATUSES.has(row.status) && (!end || end > todayOnly)) {
      addWarning(row.id, `This engagement is ${row.status}, but the assignment's end date hasn't passed yet.`);
    }

    if (row.status === EngagementStatus.IN_PROGRESS && end && end < todayOnly) {
      addWarning(row.id, "This assignment's end date has already passed, but the engagement is still In Progress.");
    }
  }

  for (let i = 0; i < rows.length; i++) {
    for (let j = i + 1; j < rows.length; j++) {
      const a = rows[i];
      const b = rows[j];
      const aStart = new Date(a.startDate).getTime();
      const aEnd = a.endDate ? new Date(a.endDate).getTime() : Infinity;
      const bStart = new Date(b.startDate).getTime();
      const bEnd = b.endDate ? new Date(b.endDate).getTime() : Infinity;

      if (aStart <= bEnd && bStart <= aEnd) {
        addWarning(a.id, `Overlaps with the assignment on "${b.engagementName}".`);
        addWarning(b.id, `Overlaps with the assignment on "${a.engagementName}".`);
      }
    }
  }

  return warningsById;
}
