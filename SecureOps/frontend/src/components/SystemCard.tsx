import { Link } from 'react-router-dom';
import { ArrowRight, Cpu, HardDrive, MemoryStick, Timer } from 'lucide-react';
import StatusBadge from './StatusBadge';
import SecurityStatus from './SecurityStatus';
import { fmtPct, fmtUptime, timeAgo } from '../utils/format';
import { loadTone } from '../utils/status';
import type { SystemOverview } from '../types';

function Gauge({ label, value, icon: Icon }: { label: string; value: number | null; icon: typeof Cpu }) {
  return (
    <div className={`gauge tone-${loadTone(value)}`}>
      <div className="gauge-head"><Icon size={14} aria-hidden="true" /><span>{label}</span><strong>{fmtPct(value)}</strong></div>
      <div className="bar" role="progressbar" aria-label={label} aria-valuenow={Math.round(value ?? 0)} aria-valuemin={0} aria-valuemax={100}>
        <div className="bar-fill" style={{ width: `${value ?? 0}%` }} />
      </div>
    </div>
  );
}

export default function SystemCard({ system }: { system: SystemOverview }) {
  const waiting = system.lastCollected == null;
  return (
    <article className="card system-card">
      <header className="sc-head">
        <div>
          <h3>{system.machineName}</h3>
          <p className="muted"><code>{system.hostname ?? 'hostname pending'}</code> · <code>{system.ipAddress}</code></p>
        </div>
        <StatusBadge status={system.enabled ? system.status : 'UNKNOWN'} label={system.enabled ? undefined : 'Disabled'} />
      </header>
      {waiting ? (
        <p className="muted pad">Waiting for first collection.</p>
      ) : (
        <>
          <Gauge label="CPU" value={system.cpuUsage} icon={Cpu} />
          <Gauge label="RAM" value={system.memoryUsage} icon={MemoryStick} />
          <Gauge label="Disk" value={system.diskUsage} icon={HardDrive} />
          <div className="sc-row"><Timer size={14} aria-hidden="true" /> Uptime <strong>{fmtUptime(system.uptimeSeconds)}</strong></div>
        </>
      )}
      <div className="sc-row">Security <StatusBadge status={system.securityStatus} size="sm" />
        {system.findingsCount > 0 && <span className="muted">{system.findingsCount} finding{system.findingsCount > 1 ? 's' : ''}</span>}</div>
      {!waiting && <SecurityStatus firewallActive={system.firewallActive} selinuxMode={system.selinuxMode} sshActive={system.sshActive} />}
      <footer className="sc-foot">
        <span className="muted">Last seen {timeAgo(system.lastSeen)}</span>
        <Link className="btn btn-ghost" to={`/systems/${system.id}`}>View details <ArrowRight size={14} aria-hidden="true" /></Link>
      </footer>
    </article>
  );
}
