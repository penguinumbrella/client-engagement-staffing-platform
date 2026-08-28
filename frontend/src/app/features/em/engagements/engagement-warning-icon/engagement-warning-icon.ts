import { Component, input } from '@angular/core';

@Component({
  selector: 'app-engagement-warning-icon',
  templateUrl: './engagement-warning-icon.html',
})
export class EngagementWarningIcon {
  readonly warnings = input.required<string[]>();
  readonly iconClass = input('text-amber-500');
  readonly direction = input<'up' | 'down' | 'left' | 'right'>('up');
}
