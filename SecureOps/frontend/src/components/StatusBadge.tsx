import { AlertTriangle, CheckCircle2, CircleHelp, Info, XCircle } from 'lucide-react';
import { statusInfo, type Tone } from '../utils/status';

const ICONS: Record<Tone, typeof Info> = { ok: CheckCircle2, warn: AlertTriangle, bad: XCircle, info: Info, muted: CircleHelp };

/** Status is conveyed by icon + text + colour, never colour alone. */
export default function StatusBadge({ status, label, size = 'md' }: { status: string | null | undefined; label?: string; size?: 'sm' | 'md' }) {
  const { tone, label: defaultLabel } = statusInfo(status);
  const Icon = ICONS[tone];
  return (
    <span className={`badge badge-${tone} badge-${size}`} data-testid="status-badge" data-tone={tone}>
      <Icon size={size === 'sm' ? 12 : 14} aria-hidden="true" />
      {label ?? defaultLabel}
    </span>
  );
}
