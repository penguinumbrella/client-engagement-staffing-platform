import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CreateEngagementRequest, Engagement, UpdateEngagementRequest } from '../types/engagement.types';

/**
 * Talks to the `engagement` service exclusively through the api-gateway
 * (`/engagement/**` -> StripPrefix=1 -> engagement service's `/api/engagements`).
 * Never call the engagement service's own port directly.
 */
@Injectable({ providedIn: 'root' })
export class EngagementService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/engagement/api/engagements`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Engagement[]> {
    return this.getAllResponse().pipe(map((response) => response.body!));
  }

  getAllResponse(): Observable<HttpResponse<Engagement[]>> {
    return this.http.get<Engagement[]>(this.baseUrl, { observe: 'response' });
  }

  getById(id: number): Observable<Engagement> {
    return this.http.get<Engagement>(`${this.baseUrl}/${id}`);
  }

  getByClient(clientId: number): Observable<Engagement[]> {
    return this.http.get<Engagement[]>(`${this.baseUrl}/client/${clientId}`);
  }

  search(q: string): Observable<Engagement[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Engagement[]>(`${this.baseUrl}/search`, { params });
  }

  create(request: CreateEngagementRequest): Observable<Engagement> {
    return this.http.post<Engagement>(this.baseUrl, request);
  }

  update(id: number, request: UpdateEngagementRequest): Observable<Engagement> {
    return this.http.put<Engagement>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, status: Engagement['status']): Observable<Engagement> {
    return this.update(id, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  cancel(id: number): Observable<Engagement> {
    return this.http.post<Engagement>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
