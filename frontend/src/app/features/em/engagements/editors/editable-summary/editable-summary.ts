import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** A paragraph that turns into a textarea on click, saving on blur and discarding on Escape. */
@Component({
  selector: 'app-editable-summary',
  imports: [FormsModule],
  templateUrl: './editable-summary.html',
  styleUrl: './editable-summary.css',
})
export class EditableSummary {
  @Input() value: string | undefined;
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('draftTextarea') private draftTextarea?: ElementRef<HTMLTextAreaElement>;

  protected editing = false;
  protected draft = '';

  protected startEdit(): void {
    this.draft = this.value ?? '';
    this.editing = true;
    // The HTML `autofocus` attribute is only honored reliably the first time an element
    // is auto-focused; browsers suppress it on later dynamic insertions once the user has
    // already interacted with the page. Focusing imperatively after render works every time.
    setTimeout(() => this.draftTextarea?.nativeElement.focus());
  }

  protected save(): void {
    if (!this.editing) {
      return;
    }
    this.editing = false;
    this.valueChange.emit(this.draft);
  }

  protected cancelEdit(): void {
    this.editing = false;
  }
}
