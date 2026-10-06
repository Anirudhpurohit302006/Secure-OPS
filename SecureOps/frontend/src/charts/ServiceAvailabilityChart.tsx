import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ServiceAvailability } from '../types';
import { AXIS, GRID, tooltipStyle } from './common';

const color = (pct: number) => (pct >= 99 ? '#34c38f' : pct >= 90 ? '#f2b84b' : '#f0616d');

export default function ServiceAvailabilityChart({ items, height }: { items: ServiceAvailability[]; height?: number }) {
  const data = items.map((i) => ({ name: `${i.machineName} · ${i.serviceName}`, pct: i.availabilityPct, samples: i.samples }));
  const h = height ?? Math.max(180, data.length * 38 + 40);
  return (
    <div role="img" aria-label="Service availability percentage" style={{ width: '100%', height: h }}>
      <ResponsiveContainer>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 0, left: 8 }}>
          <CartesianGrid stroke={GRID} horizontal={false} />
          <XAxis type="number" domain={[0, 100]} unit="%" {...AXIS} />
          <YAxis type="category" dataKey="name" width={150} {...AXIS} />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff0d' }}
            formatter={(v: number, _n, p) => [`${v}% (${(p.payload as { samples: number }).samples} samples)`, 'Availability']} />
          <Bar dataKey="pct" isAnimationActive={false} radius={[0, 4, 4, 0]}>
            {data.map((d) => <Cell key={d.name} fill={color(d.pct)} />)}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
