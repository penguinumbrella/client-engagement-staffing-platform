import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  AuthUser
} from '../../../types';

@Injectable({
  providedIn: 'root'
})
export class Auth {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8125/auth/api/auth';

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/login`,
      request
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/register`,
      request
    );
  }

  getCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(
      `${this.baseUrl}/me`
    );
  }

  saveToken(token: string): void {
    localStorage.setItem('accessToken', token);
  }

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  saveUser(user: AuthUser): void {
    localStorage.setItem(
      'user',
      JSON.stringify(user)
    );
  }

  getUser(): AuthUser | null {
    const user = localStorage.getItem('user');

    if (!user) {
      return null;
    }

    return JSON.parse(user) as AuthUser;
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
  }
}