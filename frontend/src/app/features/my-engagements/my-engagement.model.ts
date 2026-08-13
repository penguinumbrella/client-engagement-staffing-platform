import { Engagement } from '../../types/engagement.types';
import { EngagementRole } from '../../types/assignment.types';
import { ClientBadge, ConsultantBadge } from '../em/engagements/engagement-detail/engagement.model';

export interface MyEngagementRow extends Engagement {
  myRole: EngagementRole;
  teammates: ConsultantBadge[];
  client: ClientBadge;
}
