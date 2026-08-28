import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../environments/environment';
import { Client } from '../types/client.types';
import { Page } from '../types/pagination.types';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiGatewayUrl}/client/clients`;

  getAllClients(page = 0, size = 100): Observable<Page<Client>> {
    return this.getAllClientsResponse(page, size).pipe(map((response) => response.body!));
  }

  getAllClientsResponse(page = 0, size = 100): Observable<HttpResponse<Page<Client>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Client>>(this.baseUrl, { params, observe: 'response' });
  }

  getClientById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.baseUrl}/${id}`);
  }

  createClient(client: Client): Observable<Client> {
    return this.http.post<Client>(this.baseUrl, client);
  }

  updateClient(id: number, client: Client): Observable<Client> {
    return this.http.put<Client>(`${this.baseUrl}/${id}`, client);
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
