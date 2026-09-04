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
      [class.flex-1]="size() !== 'sm'"
      [class.py-2]="size() === 'sm'"
    >
      <img
        src="/images/auth/spinny-bee.png"
        alt=""
        class="animate-spin object-contain"
        [class.h-12]="size() === 'sm'"
        [class.w-12]="size() === 'sm'"
        [class.h-24]="size() !== 'sm'"
        [class.w-24]="size() !== 'sm'"
      >
    </div>
  `,
})
export class SpinnyBee {
  readonly size = input<'sm' | 'md'>('md');
}
