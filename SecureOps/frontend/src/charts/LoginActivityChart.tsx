import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { fmtDay, fmtTime } from '../utils/format';
import type { LoginBucket } from '../types';
import { AXIS, GRID, tooltipStyle } from './common';

export default function LoginActivityChart({ buckets, daily, height = 240 }: { buckets: LoginBucket[]; daily: boolean; height?: number }) {
  const data = buckets.map((b) => ({ ...b, label: daily ? fmtDay(b.bucket + 'Z') : fmtTime(b.bucket + 'Z') }));
  return (
    <div role="img" aria-label="Successful and failed SSH logins over time" style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: -18 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="label" {...AXIS} interval="preserveStartEnd" />
          <YAxis allowDecimals={false} {...AXIS} />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff0d' }} />
          <Legend />
          <Bar dataKey="success" name="Successful" stackId="a" fill="#34c38f" isAnimationActive={false} />
          <Bar dataKey="failed" name="Failed" stackId="a" fill="#f0616d" isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
