/** Values are the backend's `@JsonValue` labels (see engagement service's EngagementStatus enum), not the Java constant names. */
export enum EngagementStatus {
  PLANNED = 'Planned',
  IN_PROGRESS = 'In Progress',
  ON_HOLD = 'On Hold',
  COMPLETED = 'Completed',
}

/** Values are the backend's `@JsonValue` labels (see engagement service's EngagementType enum), not the Java constant names. */
export enum EngagementType {
  AUDIT = 'Audit',
  TAX_ADVISORY = 'Tax Advisory',
  RISK_CONSULTING = 'Risk Consulting',
  FINANCIAL_ADVISORY = 'Financial Advisory',
}

export interface Engagement {
  id: number;
  engagementName: string;
  clientId: number;
  engagementType: EngagementType;
  summary?: string;
  startDate: string;
  targetEndDate: string;
  status: EngagementStatus;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Matches engagement service's CreateEngagementRequest. */
export interface CreateEngagementRequest {
  engagementName: string;
  clientId: number;
  engagementType: EngagementType;
  summary?: string;
  startDate: string;
  targetEndDate: string;
  status?: EngagementStatus;
}

/** Matches engagement service's UpdateEngagementRequest — partial update, only these fields are editable; omitted/null fields are left unchanged server-side. */
export interface UpdateEngagementRequest {
  engagementType?: EngagementType;
  summary?: string;
  startDate?: string;
  targetEndDate?: string;
  status?: EngagementStatus;
}
