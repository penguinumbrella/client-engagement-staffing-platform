import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../types/notification.types';

@Component({
  selector: 'app-notifications-modal',
  imports: [],
  templateUrl: './notifications-modal.html',
  styleUrl: './notifications-modal.css',
})
export class NotificationsModal implements OnInit {
  @Input({ required: true }) recipientId!: string;
  @Output() close = new EventEmitter<void>();
  @Output() unreadCountChange = new EventEmitter<number>();

  protected notifications: Notification[] = [];
  protected loading = true;

  constructor(private readonly notificationService: NotificationService) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;

    this.notificationService.getForRecipient(this.recipientId).subscribe({
      next: notifications => {
        this.notifications = notifications
          .slice()
          .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.loading = false;
        this.emitUnreadCount();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  protected markAsRead(notification: Notification): void {
    if (notification.read) {
      return;
    }

    this.notificationService.markAsRead(notification.id).subscribe(updated => {
      notification.read = updated.read;
      this.emitUnreadCount();
    });
  }

  private emitUnreadCount(): void {
    this.unreadCountChange.emit(this.notifications.filter(n => !n.read).length);
  }

  protected typeLabel(type: Notification['type']): string {
    switch (type) {
      case 'ENGAGEMENT_CREATED': return 'Engagement Created';
      case 'ENGAGEMENT_UPDATED': return 'Engagement Updated';
      case 'ENGAGEMENT_CANCELLED': return 'Engagement Cancelled';
      case 'ENGAGEMENT_DELETED': return 'Engagement Deleted';
      case 'ASSIGNMENT_CREATED': return 'Assignment Created';
      case 'ASSIGNMENT_UPDATED': return 'Assignment Updated';
      case 'ASSIGNMENT_REMOVED': return 'Assignment Removed';
      case 'ASSIGNMENT_CANCELLED': return 'Assignment Cancelled';
      default: return 'General';
    }
  }
}
