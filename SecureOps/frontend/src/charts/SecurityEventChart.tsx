import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { fmtDay, fmtTime } from '../utils/format';
import type { SecurityBucket } from '../types';
import { AXIS, GRID, tooltipStyle } from './common';

export const SEVERITY_COLORS = { info: '#4c9aff', low: '#7aa7d9', medium: '#f2b84b', high: '#f0616d', critical: '#c0264b' };

export default function SecurityEventChart({ buckets, daily, height = 240 }: { buckets: SecurityBucket[]; daily: boolean; height?: number }) {
  const data = buckets.map((b) => ({ ...b, label: daily ? fmtDay(b.bucket + 'Z') : fmtTime(b.bucket + 'Z') }));
  return (
    <div role="img" aria-label="Security events by severity over time" style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: -18 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="label" {...AXIS} interval="preserveStartEnd" />
          <YAxis allowDecimals={false} {...AXIS} />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff0d' }} />
          <Legend />
          {(['info', 'low', 'medium', 'high', 'critical'] as const).map((k) => (
            <Bar key={k} dataKey={k} name={k.toUpperCase()} stackId="s" fill={SEVERITY_COLORS[k]} isAnimationActive={false} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
