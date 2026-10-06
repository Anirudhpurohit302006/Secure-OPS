import { useMemo, useState } from 'react';
import { ShieldAlert } from 'lucide-react';
import { Card } from '../components/ChartCard';
import DataTable, { type Column } from '../components/DataTable';
import PageHeader from '../components/PageHeader';
import RangeSelect from '../components/RangeSelect';
import SecurityStatus from '../components/SecurityStatus';
import StatusBadge from '../components/StatusBadge';
import { Async, EmptyState } from '../components/states';
import { useTargets } from '../hooks/TargetContext';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';
import { fmtDateTime } from '../utils/format';
import { eventLabel, statusInfo } from '../utils/status';
import type { AuthEvent, ListeningPort, RangeKey, SecurityEvent, SecurityFinding, SecurityResponse, SecurityStatus as SecStatus, Severity } from '../types';

const RANK: Record<Severity, number> = { INFO: 0, LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };

const findingCols: Column<SecurityFinding>[] = [
  { key: 'sev', header: 'Severity', render: (r) => <StatusBadge status={r.severity} size="sm" /> },
  { key: 'type', header: 'Finding', render: (r) => <code>{r.type}</code> },
  { key: 'desc', header: 'Description', render: (r) => r.description },
  { key: 'tgt', header: 'Target', render: (r) => r.machineName },
  { key: 'ts', header: 'Assessed', render: (r) => fmtDateTime(r.timestamp) },
];
const authCols: Column<AuthEvent>[] = [
  { key: 'type', header: 'Event', render: (r) => <StatusBadge status={r.eventType === 'SSH_LOGIN_FAILED' ? 'LOW' : 'INFO'} label={eventLabel(r.eventType)} size="sm" /> },
  { key: 'user', header: 'Username', render: (r) => <code>{r.username}</code> },
  { key: 'ip', header: 'Source IP', render: (r) => <code>{r.sourceIp}</code> },
  { key: 'tgt', header: 'Target', render: (r) => r.machineName },
  { key: 'ts', header: 'Timestamp', render: (r) => fmtDateTime(r.timestamp) },
];
const eventCols: Column<SecurityEvent>[] = [
  { key: 'sev', header: 'Severity', render: (r) => <StatusBadge status={r.severity} size="sm" /> },
  { key: 'type', header: 'Type', render: (r) => eventLabel(r.eventType) },
  { key: 'desc', header: 'Description', render: (r) => r.description },
  { key: 'tgt', header: 'Target', render: (r) => r.machineName },
  { key: 'ts', header: 'Timestamp', render: (r) => fmtDateTime(r.timestamp) },
];
const portCols: Column<ListeningPort>[] = [
  { key: 'p', header: 'Port', render: (r) => <code>{r.port}</code> },
  { key: 'pr', header: 'Protocol', render: (r) => r.protocol.toUpperCase() },
  { key: 'b', header: 'Bound to', render: (r) => <code>{r.bindAddress}</code> },
  { key: 's', header: 'State', render: (r) => r.state },
];

function overall(list: SecurityResponse[]): SecStatus {
  if (list.length === 0 || list.every((s) => s.overallStatus === 'UNKNOWN')) return 'UNKNOWN';
  if (list.some((s) => s.overallStatus === 'AT_RISK')) return 'AT_RISK';
  if (list.some((s) => s.overallStatus === 'WARNING')) return 'WARNING';
  return 'SECURE';
}

export default function SecurityPage() {
  const { targets, selected } = useTargets();
  const [range, setRange] = useState<RangeKey>('24h');
  const [authType, setAuthType] = useState<'' | 'SSH_LOGIN_SUCCESS' | 'SSH_LOGIN_FAILED'>('');

  const ids = useMemo(() => (selected === 'all' ? targets.map((t) => t.id) : [Number(selected)]), [selected, targets]);
  const controls = useApi(
    () => Promise.all(ids.map(async (id) => ({ id, name: targets.find((t) => t.id === id)?.machineName ?? `#${id}`, sec: await api.security(id) }))),
    [ids.join(',')], 60_000);
  const auth = useApi(() => api.authEvents(selected, range, authType || undefined), [selected, range, authType], 60_000);
  const events = useApi(() => api.securityEvents(selected, range), [selected, range], 60_000);

  return (
    <>
      <PageHeader title="Security" subtitle="Controls, findings and authentication activity"
        actions={<RangeSelect value={range} onChange={setRange} />} />

      <Async state={controls} empty={(d) => d.length === 0}>
        {(list) => {
          const status = overall(list.map((l) => l.sec));
          const findings = list.flatMap((l) => l.sec.findings).sort((a, b) => RANK[b.severity] - RANK[a.severity]);
          return (
            <>
              <section className={`banner tone-${statusInfo(status).tone}`} aria-label="Overall security status">
                <ShieldAlert size={28} aria-hidden="true" />
                <div><span className="muted">Overall security status</span><strong><StatusBadge status={status} /></strong></div>
                <div className="spacer" />
                <div><span className="muted">Open findings</span><strong>{findings.filter((f) => f.severity !== 'INFO').length}</strong></div>
              </section>

              <div className="grid cols-2">
                {list.map(({ id, name, sec }) => (
                  <Card key={id} title={name} subtitle={sec.lastCollected ? `Assessed ${fmtDateTime(sec.lastCollected)}` : 'Waiting for first collection'}
                    actions={<StatusBadge status={sec.overallStatus} />}>
                    <SecurityStatus firewallActive={sec.firewallActive} selinuxMode={sec.selinuxMode} sshActive={sec.sshActive} sshPort={sec.sshPort} />
                    {sec.sshPort != null && <p className="muted small">SSH port {sec.sshPort} is {sec.sshPortListening ? 'listening' : 'not seen in the listening-ports list'}.</p>}
                    <h3 className="mt">SSH configuration</h3>
                    {!sec.sshConfigReadable
                      ? <p className="muted">sshd_config could not be read (see documentation: sudo rule).</p>
                      : <dl className="kv compact">{Object.entries(sec.sshConfig ?? {}).map(([k, v]) => <div key={k}><dt>{k}</dt><dd><code>{v}</code></dd></div>)}
                          {Object.keys(sec.sshConfig ?? {}).length === 0 && <p className="muted">No explicit settings (defaults apply).</p>}</dl>}
                    <h3 className="mt">Listening ports</h3>
                    <DataTable columns={portCols} rows={sec.ports} rowKey={(r) => `${r.protocol}${r.port}${r.bindAddress}`} emptyTitle="No ports collected yet" caption={`${name} listening ports`} />
                  </Card>
                ))}
              </div>

              <Card title="Security findings" subtitle="Current assessment, most severe first">
                {findings.length === 0
                  ? <EmptyState title="No findings" hint="Nothing to report, or waiting for first collection." />
                  : <DataTable columns={findingCols} rows={findings} rowKey={(r) => `${r.machineId}-${r.type}-${r.description}`} caption="Security findings" />}
              </Card>
            </>
          );
        }}
      </Async>

      <Card title="Authentication activity" subtitle="SSH logins read from /var/log/secure"
        actions={
          <label className="field-inline"><span>Show</span>
            <select value={authType} onChange={(e) => setAuthType(e.target.value as typeof authType)}>
              <option value="">All logins</option><option value="SSH_LOGIN_SUCCESS">Successful</option><option value="SSH_LOGIN_FAILED">Failed</option>
            </select>
          </label>}>
        <Async state={auth} empty={(d) => d.length === 0}>{(d) => <DataTable columns={authCols} rows={d} rowKey={(r) => r.id} caption="SSH authentication events" />}</Async>
      </Card>

      <Card title="Security event history" subtitle="Persisted findings and detections">
        <Async state={events} empty={(d) => d.length === 0}>{(d) => <DataTable columns={eventCols} rows={d} rowKey={(r) => r.id} caption="Security events" />}</Async>
      </Card>
    </>
  );
}
