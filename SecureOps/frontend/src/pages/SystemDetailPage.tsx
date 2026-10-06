import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import ChartCard, { Card } from '../components/ChartCard';
import DataTable, { type Column } from '../components/DataTable';
import EventTimeline from '../components/EventTimeline';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import RangeSelect from '../components/RangeSelect';
import ServiceStatus from '../components/ServiceStatus';
import StatusBadge from '../components/StatusBadge';
import { Async, ErrorState } from '../components/states';
import CpuChart from '../charts/CpuChart';
import DiskChart from '../charts/DiskChart';
import MemoryChart from '../charts/MemoryChart';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';
import { fmtBytes, fmtDateTime, fmtPct, fmtUptime, timeAgo } from '../utils/format';
import { loadTone } from '../utils/status';
import type { AnalyticsPoint, ListeningPort, NetworkMetric, RangeKey, ServiceHistoryPoint, SystemMetric } from '../types';

const toPoints = (rows: SystemMetric[], name: string, pick: (m: SystemMetric) => number | null): AnalyticsPoint[] =>
  rows.map((m) => ({ timestamp: m.timestamp, machineId: 0, machineName: name, value: pick(m), value2: null }));

const ifaceCols: Column<NetworkMetric>[] = [
  { key: 'n', header: 'Interface', render: (r) => <code>{r.name}</code> },
  { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.state === 'UP' ? 'ACTIVE' : r.state === 'DOWN' ? 'INACTIVE' : 'UNKNOWN'} label={r.state ?? 'unknown'} size="sm" /> },
  { key: 'ip', header: 'IP address', render: (r) => <code>{r.ipAddress || '—'}</code> },
  { key: 'rx', header: 'RX total', align: 'right', render: (r) => fmtBytes(r.rxBytes) },
  { key: 'tx', header: 'TX total', align: 'right', render: (r) => fmtBytes(r.txBytes) },
];
const portCols: Column<ListeningPort>[] = [
  { key: 'p', header: 'Port', render: (r) => <code>{r.port}</code> },
  { key: 'pr', header: 'Protocol', render: (r) => r.protocol.toUpperCase() },
  { key: 'b', header: 'Bound to', render: (r) => <code>{r.bindAddress}</code> },
  { key: 's', header: 'State', render: (r) => r.state },
];
const histCols: Column<ServiceHistoryPoint>[] = [
  { key: 't', header: 'Time', render: (r) => fmtDateTime(r.timestamp) },
  { key: 's', header: 'Service', render: (r) => <code>{r.serviceName}</code> },
  { key: 'st', header: 'State', render: (r) => <StatusBadge status={r.status} size="sm" /> },
];

/** Keep only state changes (plus each service's first sample). */
function changesOnly(rows: ServiceHistoryPoint[]): ServiceHistoryPoint[] {
  const last = new Map<string, string>();
  const out: ServiceHistoryPoint[] = [];
  for (const r of rows) {
    if (last.get(r.serviceName) !== r.status) out.push(r);
    last.set(r.serviceName, r.status);
  }
  return out.reverse();
}

export default function SystemDetailPage() {
  const id = Number(useParams().id);
  const [range, setRange] = useState<RangeKey>('24h');
  const sys = useApi(() => api.system(id), [id], 60_000);
  const metrics = useApi(() => api.systemMetrics(id, range), [id, range]);
  const net = useApi(() => api.network(id), [id]);
  const ports = useApi(() => api.ports(id), [id]);
  const svcHistory = useApi(() => api.serviceHistory(id, range), [id, range]);
  const events = useApi(() => api.dashboardEvents(String(id), 15), [id], 60_000);

  if (Number.isNaN(id)) return <ErrorState error="Invalid system id." />;
  const name = sys.data?.machineName ?? `System ${id}`;
  const empty = (d: SystemMetric[]) => d.length === 0;

  return (
    <>
      <PageHeader
        title={name}
        subtitle={sys.data && <><code>{sys.data.ipAddress}</code> · last seen {timeAgo(sys.data.lastSeen)}</>}
        actions={<><StatusBadge status={sys.data?.status} /><RangeSelect value={range} onChange={setRange} />
          <Link className="btn btn-ghost" to="/systems"><ArrowLeft size={14} aria-hidden="true" /> Systems</Link></>}
      />
      <Async state={sys}>
        {(s) => (
          <>
            <div className="grid cols-4">
              <MetricCard label="CPU" value={fmtPct(s.cpuUsage)} tone={loadTone(s.cpuUsage)} progress={s.cpuUsage} />
              <MetricCard label="RAM" value={fmtPct(s.memoryUsage)} tone={loadTone(s.memoryUsage)} progress={s.memoryUsage} />
              <MetricCard label="Disk" value={fmtPct(s.diskUsage)} tone={loadTone(s.diskUsage)} progress={s.diskUsage} />
              <MetricCard label="Uptime" value={fmtUptime(s.uptimeSeconds)} tone="info" />
            </div>
            <Card title="System information">
              <dl className="kv">
                <div><dt>Hostname</dt><dd>{s.hostname ?? '—'}</dd></div>
                <div><dt>Operating system</dt><dd>{s.osName ?? '—'}</dd></div>
                <div><dt>OS version</dt><dd>{s.osVersion || '—'}</dd></div>
                <div><dt>Kernel</dt><dd>{s.kernelVersion ?? '—'}</dd></div>
                <div><dt>IP address</dt><dd>{s.ipAddress}</dd></div>
                <div><dt>Machine ID</dt><dd><code>{s.machineUid ?? '—'}</code></dd></div>
              </dl>
            </Card>
          </>
        )}
      </Async>

      <div className="grid cols-3">
        <ChartCard title="CPU history" state={metrics} empty={empty}>{(d) => <CpuChart points={toPoints(d, name, (m) => m.cpuUsage)} height={200} />}</ChartCard>
        <ChartCard title="RAM history" state={metrics} empty={empty}>{(d) => <MemoryChart points={toPoints(d, name, (m) => m.memoryUsage)} height={200} />}</ChartCard>
        <ChartCard title="Disk history" state={metrics} empty={empty}>{(d) => <DiskChart points={toPoints(d, name, (m) => m.diskUsage)} height={200} />}</ChartCard>
      </div>

      <div className="grid cols-2">
        <Card title="Network interfaces" subtitle={net.data?.timestamp ? `Counters as of ${fmtDateTime(net.data.timestamp)}` : undefined}>
          <Async state={net} empty={(d) => d.interfaces.length === 0}>{(d) => <DataTable columns={ifaceCols} rows={d.interfaces} rowKey={(r) => r.name} caption="Network interfaces" />}</Async>
        </Card>
        <Card title="Listening ports">
          <Async state={ports} empty={(d) => d.length === 0}>{(d) => <DataTable columns={portCols} rows={d} rowKey={(r) => `${r.protocol}-${r.port}-${r.bindAddress}`} caption="Listening ports" />}</Async>
        </Card>
      </div>

      <div className="grid cols-2">
        <Card title="Services"><Async state={sys}>{(s) => <ServiceStatus services={s.services} />}</Async></Card>
        <Card title="Service history" subtitle="State changes in the selected period">
          <Async state={svcHistory} empty={(d) => d.length === 0}>{(d) => <DataTable columns={histCols} rows={changesOnly(d).slice(0, 25)} rowKey={(r) => r.serviceName + r.timestamp} caption="Service state changes" />}</Async>
        </Card>
      </div>

      <Card title="Event history"><Async state={events} empty={(d) => d.length === 0}>{(d) => <EventTimeline events={d} />}</Async></Card>
    </>
  );
}
