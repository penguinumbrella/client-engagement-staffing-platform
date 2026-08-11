/** Values are the backend's `@JsonValue` labels (see staffing service's EngagementRole enum), not the Java constant names. */
export enum EngagementRole {
  LEAD = 'Lead',
  SENIOR_ASSOCIATE = 'Senior Associate',
  ASSOCIATE = 'Associate',
}

/** Values are the backend's `@JsonValue` labels (see staffing service's AssignmentStatus enum), not the Java constant names. */
export enum AssignmentStatus {
  ACTIVE = 'Active',
  PENDING = 'Pending',
  COMPLETED = 'Completed',
  CANCELLED = 'Cancelled',
}

export interface Assignment {
  id: number;
  consultantId: number;
  consultantName: string;
  engagementId: number;
  engagementRole: EngagementRole;
  assignmentStartDate: string;
  assignmentEndDate: string;
  status: AssignmentStatus;
  statusOverridden: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Matches staffing service's CreateAssignmentRequest. */
export interface CreateAssignmentRequest {
  consultantId: number;
  engagementId: number;
  engagementRole: EngagementRole;
  assignmentStartDate: string;
  assignmentEndDate: string;
  status?: AssignmentStatus;
}

/** Matches staffing service's UpdateAssignmentStatusRequest. Manually setting status marks it overridden — future engagement-status cascades won't touch it again. */
export interface UpdateAssignmentStatusRequest {
  status: AssignmentStatus;
}
