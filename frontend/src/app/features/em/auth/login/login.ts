import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';

import { Auth } from '../auth';
import { UserRole } from '../../../../types';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './login.html'
})
export class Login {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  loading = false;

  loginForm = this.fb.nonNullable.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email
      ]
    ],

    password: [
      '',
      [
        Validators.required
      ]
    ]
  });

  login(): void {

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.auth.login(
      this.loginForm.getRawValue()
    ).subscribe({

      next: response => {

        this.auth.saveToken(
          response.accessToken
        );

        this.auth.saveUser(
          response.user
        );

        this.loading = false;

        if (
          response.user.role ===
          UserRole.ENGAGEMENT_MANAGER
        ) {

          this.router.navigate([
            '/em/clients'
          ]);

        } else {

          this.router.navigate([
            '/my-engagements'
          ]);
        }
      },

      error: error => {

        this.loading = false;

        if (error.status === 401) {
          this.messageService.add({
            severity: 'error',
            summary: 'Login Failed',
            detail: 'Invalid email or password.'
          });
        } else if (error.status === 503) {
          this.messageService.add({
            severity: 'error',
            summary: 'Service Unavailable',
            detail: error?.error?.message ?? 'The auth service is currently unavailable. Please try again later.'
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Login Failed',
            detail: 'Unable to log in. Please try again.'
          });
        }

        console.error(error);
        this.loginForm.reset();
      }
    });
  }
}