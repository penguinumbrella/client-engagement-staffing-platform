export enum UserRole {
  CONSULTANT = 'CONSULTANT',
  ENGAGEMENT_MANAGER = 'ENGAGEMENT_MANAGER',
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}
