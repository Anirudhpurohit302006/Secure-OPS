import { useState } from 'react';
import ChartCard, { Card } from '../components/ChartCard';
import DataTable, { type Column } from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import RangeSelect from '../components/RangeSelect';
import StatusBadge from '../components/StatusBadge';
import TargetSelector from '../components/TargetSelector';
import CpuChart from '../charts/CpuChart';
import DiskChart from '../charts/DiskChart';
import LoginActivityChart from '../charts/LoginActivityChart';
import MemoryChart from '../charts/MemoryChart';
import NetworkChart from '../charts/NetworkChart';
import SecurityEventChart from '../charts/SecurityEventChart';
import ServiceAvailabilityChart from '../charts/ServiceAvailabilityChart';
import { useTargets } from '../hooks/TargetContext';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';
import { fmtDateTime } from '../utils/format';
import type { AuthAnalytics, RangeKey, SecurityAnalytics, ServiceAnalytics, ServiceHistoryEntry, SourceCount } from '../types';

type Metric = 'cpu' | 'memory' | 'disk' | 'network' | 'services' | 'logins' | 'security';
const METRICS: { value: Metric; label: string; group: string }[] = [
  { value: 'cpu', label: 'CPU', group: 'Resources' }, { value: 'memory', label: 'RAM', group: 'Resources' },
  { value: 'disk', label: 'Disk', group: 'Resources' }, { value: 'network', label: 'Network RX/TX', group: 'Resources' },
  { value: 'services', label: 'Service availability', group: 'Services' },
  { value: 'logins', label: 'Login activity', group: 'Security' }, { value: 'security', label: 'Security events', group: 'Security' },
];

const srcCols = (head: string): Column<SourceCount>[] => [
  { key: 'k', header: head, render: (r) => <code>{r.key}</code> }, { key: 'c', header: 'Failed attempts', align: 'right', render: (r) => r.count },
];
const histCols: Column<ServiceHistoryEntry>[] = [
  { key: 't', header: 'Time', render: (r) => fmtDateTime(r.timestamp) }, { key: 'm', header: 'System', render: (r) => r.machineName },
  { key: 's', header: 'Service', render: (r) => <code>{r.serviceName}</code> },
  { key: 'st', header: 'State', render: (r) => <StatusBadge status={r.status} size="sm" /> },
];

/** All analytics read historical PostgreSQL data through the backend; nothing here triggers SSH. */
export default function AnalyticsPage() {
  const { selected } = useTargets();
  const [metric, setMetric] = useState<Metric>('cpu');
  const [range, setRange] = useState<RangeKey>('24h');
  const daily = range === '7d' || range === '30d';
  const none = (d: unknown[]) => d.length === 0;

  const series = useApi(
    () => (['cpu', 'memory', 'disk', 'network'].includes(metric) ? api.analyticsSeries(metric as 'cpu', selected, range) : Promise.resolve([])),
    [metric, selected, range]);
  // Only the selected metric is fetched; the others resolve to an empty value without calling the API.
  const services = useApi<ServiceAnalytics>(() => (metric === 'services' ? api.analyticsServices(selected, range) : Promise.resolve({ availability: [], history: [] })), [metric, selected, range]);
  const logins = useApi<AuthAnalytics>(() => (metric === 'logins' ? api.analyticsAuth(selected, range) : Promise.resolve({ timeline: [], topFailedSources: [], topFailedUsers: [], totalSuccess: 0, totalFailed: 0 })), [metric, selected, range]);
  const sec = useApi<SecurityAnalytics>(() => (metric === 'security' ? api.analyticsSecurity(selected, range) : Promise.resolve({ bySeverity: [], timeline: [], byType: [] })), [metric, selected, range]);

  return (
    <>
      <PageHeader title="Analytics" subtitle="Historical trends from collected data" />
      <Card>
        <div className="filters">
          <label className="field-inline"><span>Target</span><TargetSelector /></label>
          <RangeSelect value={range} onChange={setRange} />
          <label className="field-inline"><span>Metric</span>
            <select value={metric} onChange={(e) => setMetric(e.target.value as Metric)}>
              {['Resources', 'Services', 'Security'].map((g) => (
                <optgroup key={g} label={g}>{METRICS.filter((m) => m.group === g).map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}</optgroup>
              ))}
            </select>
          </label>
        </div>
      </Card>

      {metric === 'cpu' && <ChartCard title="CPU utilization" state={series} empty={none} height={340}>{(d) => <CpuChart points={d} height={340} />}</ChartCard>}
      {metric === 'memory' && <ChartCard title="Memory utilization" state={series} empty={none} height={340}>{(d) => <MemoryChart points={d} height={340} />}</ChartCard>}
      {metric === 'disk' && <ChartCard title="Disk utilization" state={series} empty={none} height={340}>{(d) => <DiskChart points={d} height={340} />}</ChartCard>}
      {metric === 'network' && <ChartCard title="Network traffic" subtitle="Average rate between collections; counter resets after reboots are skipped" state={series} empty={none} height={340}>{(d) => <NetworkChart points={d} height={340} />}</ChartCard>}

      {metric === 'services' && (
        <>
          <ChartCard title="Service availability" subtitle="Share of collections where the service was running" state={services} empty={(d) => d.availability.length === 0}>
            {(d) => <ServiceAvailabilityChart items={d.availability} />}
          </ChartCard>
          <Card title="State changes">
            {services.data && <DataTable columns={histCols} rows={services.data.history.slice(0, 50)} rowKey={(r) => `${r.machineId}${r.serviceName}${r.timestamp}`} emptyTitle="No data in this period" caption="Service state changes" />}
          </Card>
        </>
      )}

      {metric === 'logins' && (
        <>
          <div className="grid cols-3">
            <MetricCard label="Successful logins" value={logins.data?.totalSuccess ?? '—'} tone="ok" />
            <MetricCard label="Failed logins" value={logins.data?.totalFailed ?? '—'} tone={logins.data && logins.data.totalFailed > 0 ? 'warn' : 'ok'} />
          </div>
          <ChartCard title="Login activity" subtitle={daily ? 'Per day' : 'Per hour'} state={logins} empty={(d) => d.totalSuccess + d.totalFailed === 0} height={300}>
            {(d) => <LoginActivityChart buckets={d.timeline} daily={daily} height={300} />}
          </ChartCard>
          {logins.data && (
            <div className="grid cols-2">
              <Card title="Top failed-login sources"><DataTable columns={srcCols('Source IP')} rows={logins.data.topFailedSources} rowKey={(r) => r.key} emptyTitle="No failed logins" /></Card>
              <Card title="Top targeted usernames"><DataTable columns={srcCols('Username')} rows={logins.data.topFailedUsers} rowKey={(r) => r.key} emptyTitle="No failed logins" /></Card>
            </div>
          )}
        </>
      )}

      {metric === 'security' && (
        <>
          {sec.data && <div className="grid cols-5 tight">{sec.data.bySeverity.map((s) =>
            <MetricCard key={s.severity} label={s.severity} value={s.count} tone={s.count === 0 ? 'muted' : s.severity === 'INFO' || s.severity === 'LOW' ? 'info' : s.severity === 'MEDIUM' ? 'warn' : 'bad'} />)}</div>}
          <ChartCard title="Security events" subtitle={daily ? 'Per day, by severity' : 'Per hour, by severity'} state={sec} empty={(d) => d.bySeverity.every((s) => s.count === 0)} height={300}>
            {(d) => <SecurityEventChart buckets={d.timeline} daily={daily} height={300} />}
          </ChartCard>
          {sec.data && <Card title="Most frequent event types"><DataTable columns={[
            { key: 'k', header: 'Event type', render: (r: SourceCount) => <code>{r.key}</code> },
            { key: 'c', header: 'Count', align: 'right' as const, render: (r: SourceCount) => r.count }]} rows={sec.data.byType} rowKey={(r) => r.key} emptyTitle="No events" /></Card>}
        </>
      )}
    </>
  );
}
