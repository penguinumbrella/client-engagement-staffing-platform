import { Component, input } from '@angular/core';

@Component({
  selector: 'app-spinny-bee',
  host: {
    style: 'display: contents',
  },
  template: `
    <div
      role="status"
      aria-label="Loading"
      class="flex w-full items-center justify-center"
      [class.h-full]="size() !== 'sm'"
      [class.min-h-full]="size() !== 'sm'"
      [class.flex-1]="size() !== 'sm'"
      [class.py-2]="size() === 'sm'"
    >
      <img
        src="/images/auth/spinny-bee.png"
        alt=""
        class="spinny-bee object-contain"
        [class.h-12]="size() === 'sm'"
        [class.w-12]="size() === 'sm'"
        [class.h-28]="size() !== 'sm'"
        [class.w-28]="size() !== 'sm'"
      >
    </div>
  `,
})
export class SpinnyBee {
  readonly size = input<'sm' | 'md'>('md');
}
