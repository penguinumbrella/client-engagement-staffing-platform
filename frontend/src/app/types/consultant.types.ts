export enum SkillArea {
  AUDIT = 'Audit',
  TAX = 'Tax',
  RISK = 'Risk',
  TECHNOLOGY = 'Technology',
  STRATEGY = 'Strategy',
}

export interface Consultant {
  id: number;
  name: string;
  titleRole: string;
  primarySkillArea: SkillArea;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConsultantRequest {
  name: string;
  email: string;
  titleRole: string;
  primarySkillArea: SkillArea;
}
