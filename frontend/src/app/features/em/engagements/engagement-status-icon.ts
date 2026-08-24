import { EngagementStatus } from '../../../types/engagement.types';

/** PrimeIcons class (minus the `pi ` prefix) for each engagement lifecycle status. */
const STATUS_ICONS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: 'pi-calendar',
  [EngagementStatus.IN_PROGRESS]: 'pi-spinner',
  [EngagementStatus.ON_HOLD]: 'pi-pause-circle',
  [EngagementStatus.COMPLETED]: 'pi-check-circle',
  [EngagementStatus.CANCELLED]: 'pi-times-circle',
};

export function engagementStatusIcon(status: string): string {
  return STATUS_ICONS[status as EngagementStatus] ?? 'pi-circle';
}

/** Tailwind text-color class for each status icon, so Planned/In Progress/etc. read distinctly at a glance. */
const STATUS_ICON_COLORS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: 'text-blue-500',
  [EngagementStatus.IN_PROGRESS]: 'text-indigo-500',
  [EngagementStatus.ON_HOLD]: 'text-amber-500',
  [EngagementStatus.COMPLETED]: 'text-emerald-500',
  [EngagementStatus.CANCELLED]: 'text-[var(--p-surface-400)]',
};

export function engagementStatusIconColor(status: string): string {
  return STATUS_ICON_COLORS[status as EngagementStatus] ?? 'text-[var(--p-surface-300)]';
}

/** Hex equivalents of STATUS_ICON_COLORS, for contexts (e.g. inline SVG/CSS) that need an actual color value rather than a Tailwind class. */
const STATUS_HEX_COLORS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: '#3b82f6',
  [EngagementStatus.IN_PROGRESS]: '#6366f1',
  [EngagementStatus.ON_HOLD]: '#f59e0b',
  [EngagementStatus.COMPLETED]: '#10b981',
  [EngagementStatus.CANCELLED]: '#9ca3af',
};

export function engagementStatusColor(status: string): string {
  return STATUS_HEX_COLORS[status as EngagementStatus] ?? '#6b7280';
}

/** Muted pastel background for each Kanban column, so lanes read apart at a glance regardless of the active theme. */
const STATUS_PASTEL_BACKGROUNDS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: '#e6ebf0',
  [EngagementStatus.IN_PROGRESS]: '#e9e8f0',
  [EngagementStatus.ON_HOLD]: '#f0ecdf',
  [EngagementStatus.COMPLETED]: '#e3ece5',
  [EngagementStatus.CANCELLED]: '#eeeeee',
};

export function engagementColumnPastelBackground(status: string): string {
  return STATUS_PASTEL_BACKGROUNDS[status as EngagementStatus] ?? '#eeeeee';
}

/** Each column's base pastel, fading to a faint muted green on the right — a subtle nod to "progress" without a jarring hue swap. */
export function engagementColumnGradient(status: string): string {
  return `linear-gradient(to right, ${engagementColumnPastelBackground(status)}, #dfe9e1)`;
}

/** A deeper, still-muted shade of the same hue, for the column's dashed border. */
const STATUS_PASTEL_BORDERS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: '#aebbc9',
  [EngagementStatus.IN_PROGRESS]: '#b3b0c9',
  [EngagementStatus.ON_HOLD]: '#c9bd93',
  [EngagementStatus.COMPLETED]: '#a9c2af',
  [EngagementStatus.CANCELLED]: '#c7c7c7',
};

export function engagementColumnPastelBorder(status: string): string {
  return STATUS_PASTEL_BORDERS[status as EngagementStatus] ?? '#c7c7c7';
}
