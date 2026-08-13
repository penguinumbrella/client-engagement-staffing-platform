import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { EngagementCard, ConsultantBadge } from '../engagement.model';
import { RangeCalendar } from './range-calendar/range-calendar';

@Component({
  selector: 'app-engagement-detail',
  imports: [RangeCalendar],
  templateUrl: './engagement-detail.html',
  styleUrl: './engagement-detail.css',
})
export class EngagementDetail {
  @Input({ required: true }) engagement!: EngagementCard;
  @Output() close = new EventEmitter<void>();

  protected readonly expandedConsultant = signal<string | null>(null);

  protected formatDate(value: string): string {
    const [year, month, day] = value.split('-').map(Number);
    return `${month}/${day}/${year}`;
  }

  protected toggleConsultant(consultant: ConsultantBadge): void {
    this.expandedConsultant.set(this.expandedConsultant() === consultant.name ? null : consultant.name);
  }
}
