import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RelationshipStatus } from '../../../../../types/client.types';

/** The client's relationship status, shown as a colored pill that turns into a select on click, saving on change. */
@Component({
  selector: 'app-editable-relationship-status',
  imports: [FormsModule],
  templateUrl: './editable-relationship-status.html',
  styleUrl: './editable-relationship-status.css',
})
export class EditableRelationshipStatus {
  @Input({ required: true }) value!: RelationshipStatus;
  @Output() valueChange = new EventEmitter<RelationshipStatus>();

  @ViewChild('draftSelect') private draftSelect?: ElementRef<HTMLSelectElement>;

  protected readonly statuses = Object.values(RelationshipStatus);

  protected editing = false;
  protected draft: RelationshipStatus = RelationshipStatus.PROSPECTIVE;

  protected get badgeClass(): string {
    switch (this.value) {
      case RelationshipStatus.ACTIVE:
        return 'bg-green-100 text-green-700 hover:bg-green-200';
      case RelationshipStatus.PROSPECTIVE:
        return 'bg-blue-100 text-blue-700 hover:bg-blue-200';
      case RelationshipStatus.FORMER:
        return 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)] hover:bg-[var(--p-surface-600)]';
      default:
        return 'bg-[var(--p-surface-700)] text-[var(--p-surface-300)] hover:bg-[var(--p-surface-600)]';
    }
  }

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
