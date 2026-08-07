export enum EngagementRole {
  LEAD = 'LEAD',
  SENIOR_ASSOCIATE = 'SENIOR_ASSOCIATE',
  ASSOCIATE = 'ASSOCIATE',
}

export interface Assignment {
  id: number;
  consultantId: number;
  engagementId: number;
  engagementRole: EngagementRole;
  assignmentStartDate: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
