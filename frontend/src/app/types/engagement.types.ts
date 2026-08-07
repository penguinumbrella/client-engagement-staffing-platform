export enum EngagementStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
}

export enum EngagementType {
  AUDIT = 'AUDIT',
  TAX_ADVISORY = 'TAX_ADVISORY',
  RISK_CONSULTING = 'RISK_CONSULTING',
  FINANCIAL_ADVISORY = 'FINANCIAL_ADVISORY',
}

export interface Engagement {
  id: number;
  engagementName: string;
  clientId: number;
  engagementType: EngagementType;
  startDate: string;
  targetEndDate: string;
  status: EngagementStatus;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
