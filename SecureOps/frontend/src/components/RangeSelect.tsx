import type { RangeKey } from '../types';

export const RANGES: { value: RangeKey; label: string }[] = [
  { value: '6h', label: 'Last 6 hours' }, { value: '24h', label: 'Last 24 hours' },
  { value: '7d', label: 'Last 7 days' }, { value: '30d', label: 'Last 30 days' },
];

export default function RangeSelect({ value, onChange, label = 'Period' }: { value: RangeKey; onChange: (r: RangeKey) => void; label?: string }) {
  return (
    <label className="field-inline">
      <span>{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value as RangeKey)}>
        {RANGES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
      </select>
    </label>
  );
}
