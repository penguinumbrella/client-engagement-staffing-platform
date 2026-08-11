import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** The client's company name, shown as a heading that turns into a text input on click, saving on blur/Enter and discarding on Escape. */
@Component({
  selector: 'app-editable-company-name',
  imports: [FormsModule],
  templateUrl: './editable-company-name.html',
  styleUrl: './editable-company-name.css',
})
export class EditableCompanyName {
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
    if (this.draft.trim()) {
      this.valueChange.emit(this.draft.trim());
    }
  }

  protected cancelEdit(): void {
    this.editing = false;
  }
}
