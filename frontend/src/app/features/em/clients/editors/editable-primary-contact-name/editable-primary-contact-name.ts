import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** The client's primary contact name, shown as a labeled value that turns into a text input on click, saving on blur/Enter and discarding on Escape. */
@Component({
  selector: 'app-editable-primary-contact-name',
  imports: [FormsModule],
  templateUrl: './editable-primary-contact-name.html',
  styleUrl: './editable-primary-contact-name.css',
})
export class EditablePrimaryContactName {
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
