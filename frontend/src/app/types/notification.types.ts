export type NotificationType =
  | 'ENGAGEMENT_CREATED'
  | 'ENGAGEMENT_UPDATED'
  | 'ENGAGEMENT_CANCELLED'
  | 'ENGAGEMENT_DELETED'
  | 'ASSIGNMENT_CREATED'
  | 'ASSIGNMENT_UPDATED'
  | 'ASSIGNMENT_REMOVED'
  | 'ASSIGNMENT_CANCELLED'
  | 'GENERAL';

export interface Notification {
  id: number;
  recipientId: string;
  title: string;
  message: string;
  type: NotificationType;
  sourceService: string | null;
  sourceId: number | null;
  read: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
