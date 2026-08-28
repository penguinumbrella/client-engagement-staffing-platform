/** Autumn-toned palette for avatar/logo placeholders, so initials read warm and cohesive instead of a jarring rainbow. */
const AVATAR_COLORS = ['#c1440e', '#a63d2f', '#b8860b', '#6b4423', '#8b5e34', '#cc7722', '#7c2d12', '#5c5a1e'];

export function initialsOf(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}

export function colorOf(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) | 0;
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}
