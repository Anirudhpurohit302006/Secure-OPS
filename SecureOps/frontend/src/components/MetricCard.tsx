import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import type { Tone } from '../utils/status';

interface Props { label: string; value: ReactNode; sub?: ReactNode; tone?: Tone; icon?: LucideIcon; progress?: number | null }

export default function MetricCard({ label, value, sub, tone = 'muted', icon: Icon, progress }: Props) {
  return (
    <div className={`card metric tone-${tone}`}>
      <div className="metric-head">
        <span className="metric-label">{label}</span>
        {Icon && <Icon size={16} aria-hidden="true" />}
      </div>
      <div className="metric-value">{value}</div>
      {sub && <div className="metric-sub">{sub}</div>}
      {progress != null && (
        <div className="bar" role="progressbar" aria-label={label} aria-valuenow={Math.round(progress)} aria-valuemin={0} aria-valuemax={100}>
          <div className="bar-fill" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
        </div>
      )}
    </div>
  );
}
