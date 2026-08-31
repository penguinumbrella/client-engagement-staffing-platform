import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ClientService } from './ClientService';
import { EngagementService } from './engagement.service';
import { ConsultantService } from './consultant.service';
import { Client } from '../types/client.types';
import { Engagement } from '../types/engagement.types';
import { Consultant } from '../types/consultant.types';

export interface SearchResult {
  type: 'client' | 'engagement' | 'consultant';
  id: number;
  title: string;
  subtitle: string;
}

export interface GroupedSearchResults {
  clients: SearchResult[];
  engagements: SearchResult[];
  consultants: SearchResult[];
}

/**
 * Fans out to the client/engagement/staffing services' search endpoints
 * through their existing per-entity Angular services (never calls a
 * gateway/BFF search route directly - there isn't one).
 */
@Injectable({ providedIn: 'root' })
export class GlobalSearchService {
  private readonly clientService = inject(ClientService);
  private readonly engagementService = inject(EngagementService);
  private readonly consultantService = inject(ConsultantService);

  search(q: string): Observable<GroupedSearchResults> {
    return forkJoin({
      clients: this.clientService.search(q).pipe(
        map((clients) => clients.map((c) => this.toClientResult(c))),
        catchError(() => of<SearchResult[]>([])),
      ),
      engagements: this.engagementService.search(q).pipe(
        map((engagements) => engagements.map((e) => this.toEngagementResult(e))),
        catchError(() => of<SearchResult[]>([])),
      ),
      consultants: this.consultantService.search(q).pipe(
        map((consultants) => consultants.map((c) => this.toConsultantResult(c))),
        catchError(() => of<SearchResult[]>([])),
      ),
    });
  }

  private toClientResult(client: Client): SearchResult {
    return {
      type: 'client',
      id: client.id!,
      title: client.companyName,
      subtitle: client.industry,
    };
  }

  private toEngagementResult(engagement: Engagement): SearchResult {
    return {
      type: 'engagement',
      id: engagement.id,
      title: engagement.engagementName,
      subtitle: engagement.status,
    };
  }

  private toConsultantResult(consultant: Consultant): SearchResult {
    return {
      type: 'consultant',
      id: consultant.id,
      title: consultant.name,
      subtitle: consultant.titleRole,
    };
  }
}
