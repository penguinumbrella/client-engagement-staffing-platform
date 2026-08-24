import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-edit-dates-modal',
  imports: [FormsModule],
  templateUrl: './edit-dates-modal.html',
  styleUrl: './edit-dates-modal.css',
})
export class EditDatesModal implements OnInit {
  @Input({ required: true }) startDate!: string;
  @Input({ required: true }) targetEndDate!: string;
  @Output() save = new EventEmitter<{ startDate: string; targetEndDate: string }>();
  @Output() cancel = new EventEmitter<void>();

  protected startDateDraft = '';
  protected targetEndDateDraft = '';

  ngOnInit(): void {
    this.startDateDraft = this.startDate;
    this.targetEndDateDraft = this.targetEndDate;
  }

  protected submit(): void {
    if (this.startDateDraft > this.targetEndDateDraft) {
      return;
    }
    this.save.emit({ startDate: this.startDateDraft, targetEndDate: this.targetEndDateDraft });
  }
}
