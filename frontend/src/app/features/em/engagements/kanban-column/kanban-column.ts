import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { EngagementCard as EngagementCardComponent } from '../engagement-card/engagement-card';
import { EngagementCard, EngagementColumn } from '../engagement.model';
import { EngagementStatus } from '../../../../types/engagement.types';
import { engagementStatusIcon, engagementStatusIconColor } from '../engagement-status-icon';

@Component({
  selector: 'app-kanban-column',
  imports: [CdkDropList, EngagementCardComponent],
  templateUrl: './kanban-column.html',
  styleUrl: './kanban-column.css',
})
export class KanbanColumn {
  @Input({ required: true }) column!: EngagementColumn;
  @Input() connectedTo: string[] = [];
  @Output() dropped = new EventEmitter<CdkDragDrop<EngagementCard[]>>();
  @Output() select = new EventEmitter<EngagementCard>();
  @Output() add = new EventEmitter<EngagementStatus>();

  protected readonly statusIcon = engagementStatusIcon;
  protected readonly statusIconColor = engagementStatusIconColor;
}
