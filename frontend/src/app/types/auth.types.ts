import { User } from './user.types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  titleRole: string;
  primarySkillArea: string;
}

export type AuthUser = Omit<User, 'updatedAt'>;

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface LoginAttempt {
  id: string;
  userId: string | null;
  email: string;
  successful: boolean;
  failureReason: string | null;
  attemptedAt: string;
}

export interface LoginMetrics {
  totalAttempts: number;
  successfulAttempts: number;
  failedAttempts: number;
  failureRate: number;
}