import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Assignment, CreateAssignmentRequest } from '../types/assignment.types';

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

  create(request: CreateAssignmentRequest): Observable<Assignment> {
    return this.http.post<Assignment>(this.baseUrl, request);
  }
}
