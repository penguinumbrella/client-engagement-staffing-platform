export enum SkillArea {
  AUDIT = 'AUDIT',
  TAX = 'TAX',
  RISK = 'RISK',
  TECHNOLOGY = 'TECHNOLOGY',
  STRATEGY = 'STRATEGY',
}

export interface Consultant {
  id: number;
  name: string;
  titleRole: string;
  primarySkillArea: SkillArea;
  userId: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
