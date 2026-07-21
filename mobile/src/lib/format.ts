import type { UserRole } from '@/types';

export function formatRelativeTime(timestampMs: number): string {
  const diff = Date.now() - timestampMs;
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 30) {
    return new Date(timestampMs).toLocaleDateString(undefined, { month: 'short', day: '2-digit', year: 'numeric' });
  }
  if (days > 0) return `${days}d ago`;
  if (hours > 0) return `${hours}h ago`;
  if (minutes > 0) return `${minutes}m ago`;
  return 'Just now';
}

const ROLE_EMOJI: Record<UserRole, string> = {
  STUDENT: '🎓',
  RESEARCHER: '🔬',
  EDUCATOR: '📖',
  ADMIN: '🛡️',
};

export function roleEmoji(role: UserRole): string {
  return ROLE_EMOJI[role] ?? '';
}

/** Circle.banner_color is a packed ARGB int (Kotlin's Color(Int) convention). */
export function argbToCss(value: number): string {
  const a = (value >>> 24) & 0xff;
  const r = (value >>> 16) & 0xff;
  const g = (value >>> 8) & 0xff;
  const b = value & 0xff;
  return `rgba(${r}, ${g}, ${b}, ${(a / 255).toFixed(2)})`;
}
