export interface NotificationLog {

  id: number;

  recipientId: number;

  title: string;

  message: string;

  type: string;

  sourceService: string | null;

  sourceId: number | null;

  read: boolean;

  active: boolean;

  createdAt: string;

  updatedAt: string;

}