import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Assignment, CreateAssignmentRequest, UpdateAssignmentStatusRequest } from '../types/assignment.types';

/**
 * Talks to the `staffing` service exclusively through the api-gateway
 * (`/staffing/**` -> StripPrefix=1 -> staffing service's `/api/assignments`).
 * Never call the staffing service's own port directly.
 */
@Injectable({ providedIn: 'root' })
export class AssignmentService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/staffing/api/assignments`;

  constructor(private readonly http: HttpClient) {}

  getByEngagement(engagementId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.baseUrl}/engagement/${engagementId}`);
  }

  /** Every assignment ever created for this engagement, active or not. */
  getHistoryByEngagement(engagementId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.baseUrl}/engagement/${engagementId}/history`);
  }

  getByConsultant(consultantId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.baseUrl}/consultant/${consultantId}`);
  }

  create(request: CreateAssignmentRequest): Observable<Assignment> {
    return this.http.post<Assignment>(this.baseUrl, request);
  }

  updateStatus(id: number, request: UpdateAssignmentStatusRequest): Observable<Assignment> {
    return this.http.patch<Assignment>(`${this.baseUrl}/${id}/status`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
