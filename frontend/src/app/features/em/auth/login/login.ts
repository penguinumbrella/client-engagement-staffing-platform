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

  readonly googleLoginUrl = this.auth.googleLoginUrl;

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

        // Save the JWT first so the interceptor can
        // attach it to the /me request.
        this.auth.saveToken(
          response.accessToken
        );

        // Load the authenticated user from the backend
        // before navigating to their dashboard.
        this.auth.getCurrentUser().subscribe({

          next: user => {

            this.auth.saveUser(user);

            this.loading = false;

            switch (user.role) {

              case UserRole.ADMIN:

                this.router.navigate(['/admin']);

                break;

              case UserRole.ENGAGEMENT_MANAGER:

                this.router.navigate(['/em/engagements']);

                break;

              case UserRole.CONSULTANT:

                this.router.navigate(['/my-engagements']);

                break;

              default:

                this.auth.logout();

                this.messageService.add({

                  severity: 'error',

                  summary: 'Login Failed',

                  detail: 'Your account has an unsupported role.'

                });

                this.router.navigate(['/login']);

                break;

            }

          },

          error: error => {

            this.loading = false;

            // Don't leave a valid-looking token behind
            // if /me failed.
            this.auth.logout();

            this.messageService.add({

              severity: 'error',

              summary: 'Login Failed',

              detail: 'Unable to load your account information.'

            });

            console.error(
              'Failed to load current user:',
              error
            );

          }

        });

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