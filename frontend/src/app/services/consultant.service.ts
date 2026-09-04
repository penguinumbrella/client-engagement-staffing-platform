import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Consultant, CreateConsultantRequest } from '../types/consultant.types';
import { Page } from '../types/pagination.types';

function toConsultantPage(body: Page<Consultant> | Consultant[] | null): Page<Consultant> {
  if (!body) {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 0,
      first: true,
      last: true,
      empty: true,
    };
  }

  if (Array.isArray(body)) {
    return {
      content: body,
      totalElements: body.length,
      totalPages: 1,
      number: 0,
      size: body.length,
      first: true,
      last: true,
      empty: body.length === 0,
    };
  }

  return body;
}

/**
 * Talks to the `staffing` service exclusively through the api-gateway
 * (`/staffing/**` -> StripPrefix=1 -> staffing service's `/api/consultants`).
 * Never call the staffing service's own port directly.
 */
@Injectable({ providedIn: 'root' })
export class ConsultantService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/staffing/api/consultants`;

  constructor(private readonly http: HttpClient) {}

  getAll(page = 0, size = 100, skillAreas?: string[]): Observable<Consultant[]> {
    return this.getAllResponse(page, size, skillAreas).pipe(map((response) => response.body!.content));
  }

  getAllResponse(
    page = 0,
    size = 100,
    skillAreas?: string[],
  ): Observable<HttpResponse<Page<Consultant>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const skillArea of skillAreas ?? []) {
      params = params.append('skillArea', skillArea);
    }
    return this.http
      .get<Page<Consultant> | Consultant[]>(this.baseUrl, { params, observe: 'response' })
      .pipe(map((response) => response.clone({ body: toConsultantPage(response.body) })));
  }

  getById(id: number): Observable<Consultant> {
    return this.http.get<Consultant>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateConsultantRequest): Observable<Consultant> {
    return this.http.post<Consultant>(this.baseUrl, request);
  }

  search(q: string): Observable<Consultant[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Consultant[]>(`${this.baseUrl}/search`, { params });
  }
}
