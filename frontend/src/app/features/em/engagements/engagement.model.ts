import { Client } from '../../../types/client.types';
import { Consultant } from '../../../types/consultant.types';
import { Engagement, EngagementStatus } from '../../../types/engagement.types';

export type ClientBadge = Pick<Client, 'companyName'> & { initials: string; color: string };
export type ConsultantBadge = Pick<Consultant, 'name' | 'titleRole'> & { initials: string; color: string };

export interface EngagementCard extends Engagement {
  client: ClientBadge;
  consultants: ConsultantBadge[];
}

export interface EngagementColumn {
  status: EngagementStatus;
  title: string;
  engagements: EngagementCard[];
}
