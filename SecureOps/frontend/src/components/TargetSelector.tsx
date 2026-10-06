import { Server } from 'lucide-react';
import { useTargets } from '../hooks/TargetContext';

export default function TargetSelector() {
  const { targets, selected, setSelected } = useTargets();
  return (
    <label className="target-select">
      <Server size={15} aria-hidden="true" />
      <span className="sr-only">Target system</span>
      <select aria-label="Target system" value={selected} onChange={(e) => setSelected(e.target.value)}>
        <option value="all">ALL SYSTEMS</option>
        {targets.map((t) => <option key={t.id} value={String(t.id)}>{t.machineName}</option>)}
      </select>
    </label>
  );
}
