import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CdkDrag } from '@angular/cdk/drag-drop';
import { EngagementCard as EngagementCardModel } from '../engagement.model';

@Component({
  selector: 'app-engagement-card',
  imports: [CdkDrag],
  templateUrl: './engagement-card.html',
  styleUrl: './engagement-card.css',
})
export class EngagementCard {
  @Input({ required: true }) engagement!: EngagementCardModel;
  @Output() select = new EventEmitter<EngagementCardModel>();
}
