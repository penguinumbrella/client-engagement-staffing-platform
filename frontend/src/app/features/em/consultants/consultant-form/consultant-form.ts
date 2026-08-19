import { Component, inject, model, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Auth, CreateUserRequest } from '../../auth/auth';
import { SkillArea } from '../../../../types/consultant.types';

type UserRole = 'CONSULTANT' | 'ENGAGEMENT_MANAGER';

@Component({
  selector: 'app-consultant-form',
  imports: [ReactiveFormsModule],
  templateUrl: './consultant-form.html',
  styleUrl: './consultant-form.css',
})
export class ConsultantForm {
  visible = model<boolean>(false);

  saved = output<void>();
  cancel = output<void>();

  private readonly auth = inject(Auth);
  private readonly formBuilder = inject(FormBuilder);

  readonly skillAreas = Object.values(SkillArea);

  readonly userRoles = [
    { label: 'Consultant', value: 'CONSULTANT' },
    { label: 'Engagement Manager', value: 'ENGAGEMENT_MANAGER' }
  ];

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  form: FormGroup = this.formBuilder.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
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
        Validators.required,
        Validators.minLength(8)
      ]
    ],
    role: [
      'CONSULTANT' as UserRole,
      Validators.required
    ],
    titleRole: ['', Validators.required],
    primarySkillArea: [
      SkillArea.AUDIT,
      Validators.required
    ],
  });

  constructor() {
    this.form.get('role')?.valueChanges.subscribe((role) => {
      const titleRole = this.form.get('titleRole');

      if (role === 'CONSULTANT') {
        titleRole?.setValidators(Validators.required);
      } else {
        titleRole?.clearValidators();
        titleRole?.setValue('');
      }

      titleRole?.updateValueAndValidity();
    });
  }

  isConsultant(): boolean {
    return this.form.get('role')?.value === 'CONSULTANT';
  }

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.getRawValue();

    const request: CreateUserRequest = {
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      email: formValue.email.trim(),
      password: formValue.password,
      role: formValue.role
    };

    if (formValue.role === 'CONSULTANT') {
      request.titleRole = formValue.titleRole.trim();
      request.primarySkillArea = formValue.primarySkillArea;
    }

    this.submitting.set(true);

    this.auth.createUser(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.visible.set(false);
        this.resetForm();
        this.saved.emit();
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('Failed to create user. Please try again.');
      },
    });
  }

  onCancel(): void {
    this.visible.set(false);
    this.resetForm();
    this.cancel.emit();
  }

  private resetForm(): void {
    this.form.reset({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      role: 'CONSULTANT',
      titleRole: '',
      primarySkillArea: SkillArea.AUDIT
    });

    this.errorMessage.set(null);
  }
}