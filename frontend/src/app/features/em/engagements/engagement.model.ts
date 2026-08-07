import { Client } from '../../../types/client.types';
import { Consultant } from '../../../types/consultant.types';
import { Engagement, EngagementStatus } from '../../../types/engagement.types';
import { EngagementRole } from '../../../types/assignment.types';

export type ClientBadge = Pick<Client, 'companyName'> & { initials: string; color: string };

/** `titleRole` is the consultant's general job title; `projectRole` is their role on this specific engagement (from Assignment.engagementRole). */
export type ConsultantBadge = Pick<Consultant, 'name' | 'titleRole'> & {
  initials: string;
  color: string;
  projectRole: EngagementRole;
};

export interface EngagementCard extends Engagement {
  client: ClientBadge;
  consultants: ConsultantBadge[];
}

export interface EngagementColumn {
  status: EngagementStatus;
  title: string;
  engagements: EngagementCard[];
}
