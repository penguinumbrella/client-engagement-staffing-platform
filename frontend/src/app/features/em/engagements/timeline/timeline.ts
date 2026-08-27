import { Component } from '@angular/core';
import { EngagementTimeline } from '../engagement-timeline/engagement-timeline';

@Component({
  selector: 'app-timeline',
  imports: [EngagementTimeline],
  templateUrl: './timeline.html',
})
export class Timeline {}
