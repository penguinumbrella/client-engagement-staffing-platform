import { inject, Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import {
  LoginAttempt,
  LoginMetrics,
  NotificationLog
} from '../../../types';

@Injectable({
  providedIn: 'root'
})
export class Admin {

  private readonly http = inject(HttpClient);

  private readonly authAdminBaseUrl =
    'http://localhost:8125/auth/api/admin';

  private readonly notificationAdminBaseUrl =
    'http://localhost:8125/notification/api/admin';

  getLoginAttempts(): Observable<LoginAttempt[]> {

    return this.http.get<LoginAttempt[]>(
      `${this.authAdminBaseUrl}/login-attempts`
    );

  }

  getLoginMetrics(): Observable<LoginMetrics> {

    return this.http.get<LoginMetrics>(
      `${this.authAdminBaseUrl}/login-metrics`
    );

  }

  getNotificationLogs(): Observable<NotificationLog[]> {

    return this.http.get<NotificationLog[]>(
      `${this.notificationAdminBaseUrl}/notifications`
    );

  }

}