export enum RelationshipStatus {
  PROSPECTIVE = 'PROSPECTIVE',
  ACTIVE = 'ACTIVE',
  FORMER = 'FORMER',
}

export interface Client {
  id: number;
  companyName: string;
  industry: string;
  primaryContactName: string;
  primaryContactEmail: string;
  relationshipStatus: RelationshipStatus;
  isActive: boolean;
}
