import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { Admin } from '../admin';

import {
  Notification
} from '../../../../types';

@Component({

  selector: 'app-notification-logs',

  imports: [
    CommonModule
  ],

  templateUrl: './notification-logs.html',

  styleUrl: './notification-logs.css'

})
export class NotificationLogs implements OnInit {

  private readonly admin = inject(Admin);

  protected readonly notificationLogs =
    signal<Notification[]>([]);

  protected readonly loading =
    signal(true);

  protected readonly errorMessage =
    signal('');

  ngOnInit(): void {

    this.loadNotificationLogs();

  }

  private loadNotificationLogs(): void {

    this.loading.set(true);

    this.errorMessage.set('');

    this.admin
      .getNotificationLogs()
      .subscribe({

        next: logs => {

          this.notificationLogs.set(
            logs
          );

          this.loading.set(false);

        },

        error: error => {

          console.error(
            'Unable to load notification logs:',
            error
          );

          this.errorMessage.set(
            'Unable to load notification logs.'
          );

          this.loading.set(false);

        }

      });

  }

}