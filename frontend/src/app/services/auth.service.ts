import { Injectable, signal } from '@angular/core';

export interface CurrentUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly storedUser = localStorage.getItem('user');

  currentUser = signal<CurrentUser | null>(
    this.storedUser
      ? JSON.parse(this.storedUser)
      : null
  );

  setUser(user: CurrentUser): void {
    localStorage.setItem('user', JSON.stringify(user));
    this.currentUser.set(user);
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');

    this.currentUser.set(null);
  }
}