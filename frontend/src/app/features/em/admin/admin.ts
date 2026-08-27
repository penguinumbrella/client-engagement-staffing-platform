import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  LoginAttempt,
  LoginMetrics
} from '../../../types';

@Injectable({
  providedIn: 'root'
})
export class Admin {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8125/auth/api/admin';

  getLoginAttempts(): Observable<LoginAttempt[]> {
    return this.http.get<LoginAttempt[]>(
      `${this.baseUrl}/login-attempts`
    );
  }

  getLoginMetrics(): Observable<LoginMetrics> {
    return this.http.get<LoginMetrics>(
      `${this.baseUrl}/login-metrics`
    );
  }
}