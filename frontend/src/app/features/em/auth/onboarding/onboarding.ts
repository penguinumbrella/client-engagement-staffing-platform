import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';

import { Auth } from '../auth';

@Component({
  selector: 'app-onboarding',
  imports: [
    ReactiveFormsModule
  ],
  templateUrl: './onboarding.html'
})
export class Onboarding {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  loading = false;

  onboardingForm = this.fb.nonNullable.group({

    titleRole: [
      '',
      [
        Validators.required,
        Validators.maxLength(100)
      ]
    ],

    primarySkillArea: [
      '',
      [
        Validators.required
      ]
    ]
  });

  complete(): void {

    if (this.onboardingForm.invalid) {
      this.onboardingForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.auth.completeOauthProfile(
      this.onboardingForm.getRawValue()
    ).subscribe({

      next: user => {

        this.auth.saveUser(user);

        this.loading = false;

        this.router.navigate(['/my-engagements']);
      },

      error: error => {

        this.loading = false;

        this.messageService.add({
          severity: 'error',
          summary: 'Setup Failed',
          detail: 'Unable to complete your profile. Please try again.'
        });

        console.error(error);
      }
    });
  }
}
