import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  AuthUser
} from '../../../types';
import { environment } from '../../../../environments/environment';

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: 'CONSULTANT' | 'ENGAGEMENT_MANAGER';
  titleRole?: string;
  primarySkillArea?: string;
}

export interface CreateUserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'CONSULTANT' | 'ENGAGEMENT_MANAGER';
  enabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    `${environment.apiGatewayUrl}/auth/api/auth`;

  private readonly usersUrl =
  'http://localhost:8125/auth/api/users';

  currentUser = signal<AuthUser | null>(this.loadStoredUser());

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
    localStorage.setItem('user', JSON.stringify(user));

    this.currentUser.set(user);
  }

  getUser(): AuthUser | null {
    return this.currentUser();
  }

  isLoggedIn(): boolean {
    const token = this.getToken();

    if (!token) {
      return false;
    }

    try {

      const payloadPart = token.split('.')[1];

      if (!payloadPart) {
        return false;
      }

      const normalized = payloadPart
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const padded =
        normalized + '='.repeat((4 - normalized.length % 4) % 4);

      const payload = JSON.parse(atob(padded));

      if (!payload.exp) {
        return false;
      }

      return payload.exp * 1000 > Date.now();

    } catch {
      return false;
    }
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');

    this.currentUser.set(null);
  }

  private loadStoredUser(): AuthUser | null {
    const user = localStorage.getItem('user');

    if (!user) {
      return null;
    }

    return JSON.parse(user) as AuthUser;
  }

  createUser(request: CreateUserRequest): Observable<CreateUserResponse> {
    return this.http.post<CreateUserResponse>(
      this.usersUrl,
      request
    );
  }
}