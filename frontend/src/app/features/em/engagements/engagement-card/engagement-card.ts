import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CdkDrag } from '@angular/cdk/drag-drop';
import { EngagementCard as EngagementCardModel } from '../engagement-detail/engagement.model';
import { engagementStatusIcon, engagementStatusIconColor } from '../engagement-status-icon';
import { engagementWarnings } from '../engagement-warnings';
import { EngagementWarningIcon } from '../engagement-warning-icon/engagement-warning-icon';

@Component({
  selector: 'app-engagement-card',
  imports: [CdkDrag, EngagementWarningIcon],
  templateUrl: './engagement-card.html',
  styleUrl: './engagement-card.css',
})
export class EngagementCard {
  @Input({ required: true }) engagement!: EngagementCardModel;
  @Output() select = new EventEmitter<EngagementCardModel>();

  protected get warnings(): string[] {
    return engagementWarnings({
      status: this.engagement.status,
      startDate: this.engagement.startDate,
      targetEndDate: this.engagement.targetEndDate,
      consultantCount: this.engagement.consultants.length,
    });
  }
}
