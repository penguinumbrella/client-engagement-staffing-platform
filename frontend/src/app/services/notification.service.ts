import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Notification } from '../types/notification.types';

/**
 * Talks to the `notification` service exclusively through the api-gateway
 * (`/notification/**` -> StripPrefix=1 -> notification service's `/api/notifications`).
 * Never call the notification service's own port directly.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/notification/api/notifications`;

  constructor(private readonly http: HttpClient) {}

  getForRecipient(recipientId: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.baseUrl, {
      params: { recipientId }
    });
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.patch<Notification>(`${this.baseUrl}/${id}/read`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
