import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { pivotByMachine } from '../utils/format';
import type { AnalyticsPoint } from '../types';
import { AXIS, GRID, SERIES_COLORS, tickFormatter, tooltipLabel, tooltipStyle } from './common';

/** Percent line chart, one line per machine, from /api/analytics/{cpu|memory|disk}. */
export default function TimeSeriesChart({ points, name, height = 240 }: { points: AnalyticsPoint[]; name: string; height?: number }) {
  const { rows, series } = pivotByMachine(points, (p) => p.value);
  const span = rows.length > 1 ? rows[rows.length - 1].t - rows[0].t : 0;
  return (
    <div role="img" aria-label={`${name} utilisation over time`} style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <LineChart data={rows} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="t" type="number" scale="time" domain={['dataMin', 'dataMax']} tickFormatter={tickFormatter(span)} {...AXIS} />
          <YAxis domain={[0, 100]} unit="%" {...AXIS} />
          <Tooltip contentStyle={tooltipStyle} labelFormatter={tooltipLabel} formatter={(v: number) => [`${Number(v).toFixed(1)}%`, undefined]} />
          {series.length > 1 && <Legend />}
          {series.map((s, i) => (
            <Line key={s} type="monotone" dataKey={s} name={`${s} ${name}`} stroke={SERIES_COLORS[i % SERIES_COLORS.length]}
              strokeWidth={2} dot={rows.length < 40} connectNulls isAnimationActive={false} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
