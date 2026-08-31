import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Consultant, CreateConsultantRequest } from '../types/consultant.types';

/**
 * Talks to the `staffing` service exclusively through the api-gateway
 * (`/staffing/**` -> StripPrefix=1 -> staffing service's `/api/consultants`).
 * Never call the staffing service's own port directly.
 */
@Injectable({ providedIn: 'root' })
export class ConsultantService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/staffing/api/consultants`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Consultant[]> {
    return this.getAllResponse().pipe(map((response) => response.body!));
  }

  getAllResponse(): Observable<HttpResponse<Consultant[]>> {
    return this.http.get<Consultant[]>(this.baseUrl, { observe: 'response' });
  }

  create(request: CreateConsultantRequest): Observable<Consultant> {
    return this.http.post<Consultant>(this.baseUrl, request);
  }

  search(q: string): Observable<Consultant[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Consultant[]>(`${this.baseUrl}/search`, { params });
  }
}
