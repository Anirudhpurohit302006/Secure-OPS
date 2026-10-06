import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Activity, KeyRound, ServerCog, ShieldAlert, UserX, Wifi, WifiOff } from 'lucide-react';
import ChartCard, { Card } from '../components/ChartCard';
import EventTimeline from '../components/EventTimeline';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import RangeSelect from '../components/RangeSelect';
import SecurityStatus from '../components/SecurityStatus';
import ServiceStatus from '../components/ServiceStatus';
import StatusBadge from '../components/StatusBadge';
import { Async, EmptyState } from '../components/states';
import CpuChart from '../charts/CpuChart';
import MemoryChart from '../charts/MemoryChart';
import NetworkChart from '../charts/NetworkChart';
import { useTargets } from '../hooks/TargetContext';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';
import { fmtPct } from '../utils/format';
import { loadTone, statusInfo } from '../utils/status';
import type { RangeKey } from '../types';

const REFRESH = 60_000;
const CHART_REFRESH = 300_000;

export default function HomePage() {
  const { selected } = useTargets();
  const [range, setRange] = useState<RangeKey>('24h');
  const summary = useApi(() => api.dashboardSummary(selected), [selected], REFRESH);
  const cpu = useApi(() => api.analyticsSeries('cpu', selected, range), [selected, range], CHART_REFRESH);
  const mem = useApi(() => api.analyticsSeries('memory', selected, range), [selected, range], CHART_REFRESH);
  const net = useApi(() => api.analyticsSeries('network', selected, range), [selected, range], CHART_REFRESH);
  const events = useApi(() => api.dashboardEvents(selected, 12), [selected], REFRESH);
  const noHistory = (d: unknown[]) => d.length === 0;

  const s = summary.data;
  const secTone = statusInfo(s?.overallSecurity).tone;

  return (
    <>
      <PageHeader title="Home" subtitle="Infrastructure overview" actions={<RangeSelect value={range} onChange={setRange} />} />

      <div className="grid cols-4">
        <MetricCard label="Systems" value={s?.totalSystems ?? '—'} icon={ServerCog} tone="info" sub="monitored RHEL targets" />
        <MetricCard label="Online" value={s?.onlineSystems ?? '—'} icon={Wifi} tone="ok" sub="reachable at last collection" />
        <MetricCard label="Offline" value={s?.offlineSystems ?? '—'} icon={WifiOff} tone={s && s.offlineSystems > 0 ? 'bad' : 'muted'} sub="not online" />
        <MetricCard label="Security" value={<StatusBadge status={s?.overallSecurity ?? 'UNKNOWN'} />} icon={ShieldAlert} tone={secTone}
          sub={s ? `${s.securityFindings} open finding${s.securityFindings === 1 ? '' : 's'}` : undefined} />
      </div>

      <div className="grid cols-2">
        <ChartCard title="CPU utilization" subtitle="Per system, percent" state={cpu} empty={noHistory}>{(d) => <CpuChart points={d} />}</ChartCard>
        <ChartCard title="Memory utilization" subtitle="Per system, percent" state={mem} empty={noHistory}>{(d) => <MemoryChart points={d} />}</ChartCard>
      </div>

      <div className="grid cols-2">
        <Card title="Disk utilization" subtitle="Root filesystem, latest collection">
          <Async state={summary} empty={(d) => d.systems.length === 0}>
            {(d) => (
              <ul className="bars">
                {d.systems.map((sys) => (
                  <li key={sys.id}>
                    <div className="bars-head"><Link to={`/systems/${sys.id}`}>{sys.machineName}</Link><strong>{fmtPct(sys.diskUsage)}</strong></div>
                    <div className={`bar tone-${loadTone(sys.diskUsage)}`} role="progressbar" aria-label={`${sys.machineName} disk`}
                      aria-valuenow={Math.round(sys.diskUsage ?? 0)} aria-valuemin={0} aria-valuemax={100}>
                      <div className="bar-fill" style={{ width: `${sys.diskUsage ?? 0}%` }} />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Async>
        </Card>
        <ChartCard title="Network activity" subtitle="Average RX / TX rate between collections" state={net} empty={noHistory}>
          {(d) => <NetworkChart points={d} />}
        </ChartCard>
      </div>

      <div className="grid cols-2">
        <Card title="Service health" subtitle="SSH · firewalld · HTTPD">
          <Async state={summary} empty={(d) => d.systems.length === 0}>
            {(d) => d.systems.map((sys) => (
              <div key={sys.id} className="subsection">
                <h3>{sys.machineName}</h3>
                <ServiceStatus services={sys.services} />
              </div>
            ))}
          </Async>
        </Card>
        <Card title="Security summary" subtitle="Last 24 hours">
          <Async state={summary}>
            {(d) => (
              <>
                <div className="grid cols-3 tight">
                  <MetricCard label="Failed logins" value={d.failedLogins24h} icon={UserX} tone={d.failedLogins24h > 0 ? 'warn' : 'ok'} />
                  <MetricCard label="Successful logins" value={d.successfulLogins24h} icon={KeyRound} tone="info" />
                  <MetricCard label="Findings" value={d.securityFindings} icon={Activity} tone={d.securityFindings > 0 ? 'warn' : 'ok'} />
                </div>
                {d.systems.map((sys) => (
                  <div key={sys.id} className="subsection">
                    <h3>{sys.machineName}</h3>
                    <SecurityStatus firewallActive={sys.firewallActive} selinuxMode={sys.selinuxMode} sshActive={sys.sshActive} />
                  </div>
                ))}
              </>
            )}
          </Async>
        </Card>
      </div>

      <Card title="Recent events" subtitle="Logins, service failures, system and security events"
        actions={<Link className="btn btn-ghost" to="/security">Security details</Link>}>
        <Async state={events} empty={(d) => d.length === 0}>{(d) => <EventTimeline events={d} />}</Async>
        {events.data?.length === 0 && <EmptyState title="Waiting for first collection" hint="The first run starts shortly after the backend starts." />}
      </Card>
    </>
  );
}
