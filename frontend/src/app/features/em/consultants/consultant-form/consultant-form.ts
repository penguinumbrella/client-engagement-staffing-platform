import { Component, inject, model, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

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

  readonly skillAreas = Object.values(SkillArea);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  form: FormGroup = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    titleRole: ['', Validators.required],
    primarySkillArea: [SkillArea.AUDIT, Validators.required],
  });

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.consultantService.create(this.form.getRawValue()).subscribe({
      next: (consultant) => {
        this.submitting.set(false);
        this.visible.set(false);
        this.form.reset({ name: '', titleRole: '', primarySkillArea: SkillArea.AUDIT });
        this.saved.emit(consultant);
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('Failed to create consultant. Please try again.');
      },
    });
  }

  onCancel(): void {
    this.visible.set(false);
    this.form.reset({ name: '', titleRole: '', primarySkillArea: SkillArea.AUDIT });
    this.cancel.emit();
  }
}
