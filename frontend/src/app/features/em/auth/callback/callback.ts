import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';

import { Auth } from '../auth';
import { UserRole } from '../../../../types';

@Component({
  selector: 'app-auth-callback',
  imports: [],
  template: `
    <div class="h-screen flex items-center justify-center bg-[#f5f0e4]">
      <i class="pi pi-spin pi-spinner text-2xl text-[#f5a623]"></i>
    </div>
  `
})
export class Callback implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(Auth);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {

    const params = this.route.snapshot.queryParamMap;
    const token = params.get('token');
    const onboarding = params.get('onboarding') === 'true';

    if (!token) {
      this.messageService.add({
        severity: 'error',
        summary: 'Sign In Failed',
        detail: 'No token was returned from Google.'
      });

      this.router.navigate(['/login']);
      return;
    }

    this.auth.saveToken(token);

    this.auth.getCurrentUser().subscribe({

      next: user => {

        this.auth.saveUser(user);

        if (onboarding) {
          this.router.navigate(['/onboarding']);
          return;
        }

        if (user.role === UserRole.ENGAGEMENT_MANAGER) {
          this.router.navigate(['/em/clients']);
        } else {
          this.router.navigate(['/my-engagements']);
        }
      },

      error: error => {

        this.auth.logout();

        this.messageService.add({
          severity: 'error',
          summary: 'Sign In Failed',
          detail: 'Unable to complete Google sign-in.'
        });

        console.error(error);
        this.router.navigate(['/login']);
      }
    });
  }
}
