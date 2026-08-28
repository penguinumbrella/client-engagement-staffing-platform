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

/**
 * Source-of-truth status palette, shared by the timeline (bar colors) and every
 * status icon (kanban columns, engagement rows). One consistent hue range —
 * yellow through blue — walked in status order, so status colors read as a
 * single deliberate spectrum everywhere instead of unrelated ad-hoc colors.
 */
const STATUS_HEX_COLORS: Record<EngagementStatus, string> = {
  [EngagementStatus.PLANNED]: '#f6c358',
  [EngagementStatus.IN_PROGRESS]: '#8fbf6c',
  [EngagementStatus.ON_HOLD]: '#3aab8c',
  [EngagementStatus.COMPLETED]: '#55738a',
  [EngagementStatus.CANCELLED]: '#22779f',
};

export function engagementStatusColor(status: string): string {
  return STATUS_HEX_COLORS[status as EngagementStatus] ?? '#6b7280';
}

/** Hex color for a status icon — same palette as {@link engagementStatusColor}, for use with [style.color]. */
export function engagementStatusIconColor(status: string): string {
  return engagementStatusColor(status);
}

/**
 * Uniform Kanban column background, regardless of status. Resolves to a CSS custom
 * property (defined in styles.css) so it swaps automatically between light and dark mode.
 */
export function engagementColumnGradient(_status: string): string {
  return 'var(--kanban-column-bg)';
}

