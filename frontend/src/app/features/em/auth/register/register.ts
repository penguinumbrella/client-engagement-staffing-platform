import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Auth } from '../auth';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  loading = false;
  errorMessage = '';

  registerForm = this.fb.nonNullable.group(
    {

      firstName: [
        '',
        [
          Validators.required,
          Validators.maxLength(80)
        ]
      ],

      lastName: [
        '',
        [
          Validators.required,
          Validators.maxLength(80)
        ]
      ],

      email: [
        '',
        [
          Validators.required,
          Validators.email,
          Validators.maxLength(254)
        ]
      ],

      password: [
        '',
        [
          Validators.required,
          Validators.minLength(12),
          Validators.maxLength(72)
        ]
      ],

      confirmPassword: [
        '',
        [
          Validators.required
        ]
      ]

    },
    {
      validators: this.passwordsMatch
    }
  );


  passwordsMatch(
    control: AbstractControl
  ): ValidationErrors | null {

    const password =
      control.get('password')?.value;

    const confirmPassword =
      control.get('confirmPassword')?.value;

    if (password === confirmPassword) {
      return null;
    }

    return {
      passwordMismatch: true
    };
  }


  register(): void {

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }


    const {
      firstName,
      lastName,
      email,
      password
    } = this.registerForm.getRawValue();


    this.loading = true;
    this.errorMessage = '';


    this.auth.register({
      firstName,
      lastName,
      email,
      password
    }).subscribe({

      next: response => {

        this.auth.saveToken(
          response.accessToken
        );

        this.auth.saveUser(
          response.user
        );

        this.loading = false;

        this.router.navigate(['/']);
      },

      error: error => {

        this.loading = false;

        if (error.status === 409) {

          this.errorMessage =
            'An account with this email already exists.';

        } else if (error.status === 400) {

          this.errorMessage =
            'Please check the information you entered.';

        } else {

          this.errorMessage =
            'Unable to create your account. Please try again.';

        }
      }
    });
  }
}