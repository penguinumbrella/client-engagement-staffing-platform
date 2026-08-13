import { Component, Input, OnInit } from '@angular/core';

interface CalendarDay {
  date: Date;
  inCurrentMonth: boolean;
  inRange: boolean;
  isRangeStart: boolean;
  isRangeEnd: boolean;
  isToday: boolean;
}

interface MicroMonth {
  label: string;
  days: CalendarDay[];
}

const WEEKDAY_LABELS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function startOfDay(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function buildMonth(year: number, monthIndex: number, rangeStart: Date, rangeEnd: Date): MicroMonth {
  const firstOfMonth = new Date(year, monthIndex, 1);
  const gridStart = new Date(year, monthIndex, 1 - firstOfMonth.getDay());
  const today = new Date();

  const days = Array.from({ length: 42 }, (_, i) => {
    const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + i);
    return {
      date,
      inCurrentMonth: date.getMonth() === monthIndex,
      inRange: date >= rangeStart && date <= rangeEnd,
      isRangeStart: isSameDay(date, rangeStart),
      isRangeEnd: isSameDay(date, rangeEnd),
      isToday: isSameDay(date, today),
    };
  });

  return { label: `${MONTH_LABELS[monthIndex]} ${year}`, days };
}

@Component({
  selector: 'app-range-calendar',
  imports: [],
  templateUrl: './range-calendar.html',
  styleUrl: './range-calendar.css',
})
export class RangeCalendar implements OnInit {
  @Input({ required: true }) startDate!: string;
  @Input({ required: true }) endDate!: string;

  protected readonly weekdayLabels = WEEKDAY_LABELS;
  protected months: MicroMonth[] = [];

  ngOnInit(): void {
    const rangeStart = startOfDay(this.startDate);
    const rangeEnd = startOfDay(this.endDate);

    const months: MicroMonth[] = [];
    let year = rangeStart.getFullYear();
    let monthIndex = rangeStart.getMonth();

    while (year < rangeEnd.getFullYear() || (year === rangeEnd.getFullYear() && monthIndex <= rangeEnd.getMonth())) {
      months.push(buildMonth(year, monthIndex, rangeStart, rangeEnd));
      monthIndex++;
      if (monthIndex > 11) {
        monthIndex = 0;
        year++;
      }
    }

    this.months = months;
  }
}
