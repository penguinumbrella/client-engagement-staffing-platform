/** Values are the backend's `@JsonValue` labels (see staffing service's EngagementRole enum), not the Java constant names. */
export enum EngagementRole {
  LEAD = 'Lead',
  SENIOR_ASSOCIATE = 'Senior Associate',
  ASSOCIATE = 'Associate',
}

export interface Assignment {
  id: number;
  consultantId: number;
  consultantName: string;
  engagementId: number;
  engagementRole: EngagementRole;
  assignmentStartDate: string;
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
}
