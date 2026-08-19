import { Component, inject, model, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';

import { ConsultantService } from '../../../../services/consultant.service';
import { Consultant, SkillArea } from '../../../../types/consultant.types';

@Component({
  selector: 'app-consultant-form',
  imports: [ReactiveFormsModule],
  templateUrl: './consultant-form.html',
  styleUrl: './consultant-form.css',
})
export class ConsultantForm {
  visible = model<boolean>(false);

  saved = output<Consultant>();
  cancel = output<void>();

  private readonly consultantService = inject(ConsultantService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  readonly skillAreas = Object.values(SkillArea);
  readonly submitting = signal(false);

  form: FormGroup = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    email: [
      '',
      [
        Validators.required,
        Validators.email
      ]
    ],
    titleRole: ['', Validators.required],
    primarySkillArea: [
      SkillArea.AUDIT,
      Validators.required
    ],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.consultantService.create(this.form.getRawValue()).subscribe({
      next: (consultant) => {
        this.submitting.set(false);
        this.visible.set(false);
        this.form.reset({name: '', email: '', titleRole: '', primarySkillArea: SkillArea.AUDIT});
        this.saved.emit(consultant);
      },
      error: (err) => {
        this.submitting.set(false);

        if (err.status === 409) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Cannot Create',
            detail: err?.error?.message ?? 'A consultant profile already exists for this user.',
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Create Failed',
            detail: err?.error?.message ?? 'Failed to create consultant. Please try again.',
          });
        }

        console.error(err);
      },
    });
  }

  onCancel(): void {
    this.visible.set(false);
    this.form.reset({name: '', email: '', titleRole: '', primarySkillArea: SkillArea.AUDIT});
    this.cancel.emit();
  }
}
