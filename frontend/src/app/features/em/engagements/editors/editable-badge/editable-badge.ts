import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * A pill-shaped badge that turns into a native `<select>` on click, letting the
 * caller swap between a fixed set of string options (e.g. engagement type/status)
 * with a single click. Value/options are plain strings so this works for any
 * string-backed enum without the component needing to know which one.
 */
@Component({
  selector: 'app-editable-badge',
  imports: [FormsModule],
  templateUrl: './editable-badge.html',
  styleUrl: './editable-badge.css',
})
export class EditableBadge {
  @Input({ required: true }) value!: string;
  @Input({ required: true }) options: string[] = [];
  @Input() badgeClass = 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)] hover:bg-[var(--p-surface-600)]';
  @Input() selectClass = 'border-[var(--p-surface-600)] text-[var(--p-surface-300)]';
  /** Optional per-value PrimeIcons class (e.g. `'pi-check-circle'`), shown before the label. */
  @Input() icon: ((value: string) => string) | null = null;
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('draftSelect') private draftSelect?: ElementRef<HTMLSelectElement>;

  protected editing = false;
  protected draft = '';

  protected startEdit(): void {
    this.draft = this.value;
    this.editing = true;
    // The HTML `autofocus` attribute is only honored reliably the first time an element
    // is auto-focused; browsers suppress it on later dynamic insertions once the user has
    // already interacted with the page. Focusing imperatively after render works every time.
    setTimeout(() => {
      const select = this.draftSelect?.nativeElement;
      select?.focus();
      select?.showPicker?.();
    });
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
