import { Area, AreaChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { fmtRate, pivotByMachine } from '../utils/format';
import type { AnalyticsPoint } from '../types';
import { AXIS, GRID, SERIES_COLORS, tickFormatter, tooltipLabel, tooltipStyle } from './common';

/** Average RX/TX rate between collections. One RX and one TX area per machine. */
export default function NetworkChart({ points, height = 240 }: { points: AnalyticsPoint[]; height?: number }) {
  const rx = pivotByMachine(points, (p) => p.value);
  const tx = pivotByMachine(points, (p) => p.value2);
  const byT = new Map<number, Record<string, number | null>>();
  rx.rows.forEach((r) => byT.set(r.t, { t: r.t, ...Object.fromEntries(rx.series.map((s) => [`${s} RX`, r[s] ?? null])) }));
  tx.rows.forEach((r) => byT.set(r.t, { ...(byT.get(r.t) ?? { t: r.t }), ...Object.fromEntries(tx.series.map((s) => [`${s} TX`, r[s] ?? null])) }));
  const rows = Array.from(byT.values()).sort((a, b) => (a.t as number) - (b.t as number));
  const span = rows.length > 1 ? (rows[rows.length - 1].t as number) - (rows[0].t as number) : 0;
  const keys = [...rx.series.map((s) => `${s} RX`), ...tx.series.map((s) => `${s} TX`)];
  return (
    <div role="img" aria-label="Network RX and TX rate over time" style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <AreaChart data={rows} margin={{ top: 8, right: 12, bottom: 0, left: 0 }}>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="t" type="number" scale="time" domain={['dataMin', 'dataMax']} tickFormatter={tickFormatter(span)} {...AXIS} />
          <YAxis tickFormatter={(v: number) => fmtRate(v)} width={78} {...AXIS} />
          <Tooltip contentStyle={tooltipStyle} labelFormatter={tooltipLabel} formatter={(v: number) => [fmtRate(Number(v)), undefined]} />
          <Legend />
          {keys.map((k, i) => (
            <Area key={k} type="monotone" dataKey={k} stroke={SERIES_COLORS[i % SERIES_COLORS.length]}
              strokeDasharray={k.endsWith('TX') ? '5 3' : undefined}
              fill={SERIES_COLORS[i % SERIES_COLORS.length]} fillOpacity={0.12} connectNulls isAnimationActive={false} />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
