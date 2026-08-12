import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** The client's primary contact email, shown as a labeled value that turns into an email input on click, saving on blur/Enter and discarding on Escape. */
@Component({
  selector: 'app-editable-primary-contact-email',
  imports: [FormsModule],
  templateUrl: './editable-primary-contact-email.html',
  styleUrl: './editable-primary-contact-email.css',
})
export class EditablePrimaryContactEmail {
  @Input({ required: true }) value!: string;
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('draftInput') private draftInput?: ElementRef<HTMLInputElement>;

  protected editing = false;
  protected draft = '';

  protected startEdit(): void {
    this.draft = this.value;
    this.editing = true;
    // The HTML `autofocus` attribute is only honored reliably the first time an element
    // is auto-focused; browsers suppress it on later dynamic insertions once the user has
    // already interacted with the page. Focusing imperatively after render works every time.
    setTimeout(() => this.draftInput?.nativeElement.focus());
  }

  protected save(): void {
    if (!this.editing) {
      return;
    }
    if (!this.draftInput?.nativeElement.checkValidity()) {
      this.draftInput?.nativeElement.reportValidity();
      return;
    }
    this.editing = false;
    const trimmed = this.draft.trim();
    if (trimmed !== this.value) {
      this.valueChange.emit(trimmed);
    }
  }

  protected cancelEdit(): void {
    this.editing = false;
  }
}
