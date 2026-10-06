import { useState, type FormEvent } from 'react';
import { Pencil, Play, Plus, Power, RefreshCw, Trash2 } from 'lucide-react';
import { Card } from '../components/ChartCard';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';
import { Async, EmptyState } from '../components/states';
import { useTargets } from '../hooks/TargetContext';
import { api, ApiError } from '../services/api';
import { fmtDateTime } from '../utils/format';
import type { Target, TargetRequest } from '../types';

interface Notice { id: number; ok: boolean; text: string }
const blank: TargetRequest = { machineName: '', ipAddress: '', sshUsername: '', sshPort: 22, enabled: true };

export default function SettingsPage() {
  const { targets, loaded, refresh } = useTargets();
  const [editing, setEditing] = useState<Target | 'new' | null>(null);
  const [form, setForm] = useState<TargetRequest>(blank);
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice | null>(null);

  const state = { data: loaded ? targets : null, error: null, loading: !loaded, reload: refresh };

  function open(t: Target | 'new') {
    setEditing(t);
    setFormError(null);
    setForm(t === 'new' ? blank : { machineName: t.machineName, ipAddress: t.ipAddress, sshUsername: t.sshUsername, sshPort: t.sshPort, enabled: t.enabled });
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    try {
      if (editing === 'new') await api.createTarget(form);
      else if (editing) await api.updateTarget(editing.id, form);
      setEditing(null);
      refresh();
    } catch (err) { setFormError(err instanceof ApiError ? err.message : 'Could not save the target.'); }
  }

  async function act(key: string, t: Target, fn: () => Promise<{ success: boolean; message: string } | void>) {
    setBusy(key);
    try {
      const r = await fn();
      setNotice({ id: t.id, ok: r ? r.success : true, text: `${t.machineName}: ${r ? r.message : 'Done.'}` });
    } catch (err) {
      setNotice({ id: t.id, ok: false, text: `${t.machineName}: ${err instanceof ApiError ? err.message : 'Failed.'}` });
    } finally { setBusy(null); refresh(); }
  }

  const remove = (t: Target) => {
    if (window.confirm(`Delete ${t.machineName}? Its collected history will be deleted too. This cannot be undone.`))
      void act(`del-${t.id}`, t, () => api.deleteTarget(t.id));
  };

  return (
    <>
      <PageHeader title="Settings" subtitle="Target management and collection"
        actions={<button className="btn btn-primary" onClick={() => open('new')}><Plus size={15} aria-hidden="true" /> Add target</button>} />

      {notice && <div className={`notice ${notice.ok ? 'notice-ok' : 'notice-bad'}`} role="status">{notice.text}</div>}

      <Card title="RHEL targets" subtitle="SecureOps connects over SSH only, using approved read-only commands.">
        <Async state={state} empty={(d) => d.length === 0}>
          {(list) => (
            <div className="table-wrap"><table className="table">
              <caption className="sr-only">Configured targets</caption>
              <thead><tr><th scope="col">Machine</th><th scope="col">IP</th><th scope="col">SSH user</th><th scope="col">Enabled</th><th scope="col">Status</th><th scope="col">Last seen</th><th scope="col" className="right">Actions</th></tr></thead>
              <tbody>{list.map((t) => (
                <tr key={t.id}>
                  <td><strong>{t.machineName}</strong>{t.hostname && <div className="muted small">{t.hostname}</div>}</td>
                  <td><code>{t.ipAddress}</code>{t.sshPort !== 22 && <span className="muted small">:{t.sshPort}</span>}</td>
                  <td><code>{t.sshUsername}</code></td>
                  <td>{t.enabled ? 'Yes' : 'No'}</td>
                  <td><StatusBadge status={t.status} size="sm" /></td>
                  <td>{fmtDateTime(t.lastSeen)}</td>
                  <td className="right actions">
                    <button className="btn btn-ghost" disabled={busy !== null} onClick={() => void act(`test-${t.id}`, t, () => api.testTarget(t.id))}>
                      <Play size={13} aria-hidden="true" /> {busy === `test-${t.id}` ? 'Testing…' : 'Test Connection'}</button>
                    <button className="btn btn-ghost" disabled={busy !== null || !t.enabled} onClick={() => void act(`col-${t.id}`, t, () => api.collectTarget(t.id))}>
                      <RefreshCw size={13} aria-hidden="true" /> {busy === `col-${t.id}` ? 'Collecting…' : 'Collect now'}</button>
                    <button className="btn btn-ghost" onClick={() => open(t)}><Pencil size={13} aria-hidden="true" /> Edit</button>
                    <button className="btn btn-ghost" disabled={busy !== null}
                      onClick={() => void act(`en-${t.id}`, t, async () => { await api.updateTarget(t.id, { machineName: t.machineName, ipAddress: t.ipAddress, sshUsername: t.sshUsername, sshPort: t.sshPort, enabled: !t.enabled }); })}>
                      <Power size={13} aria-hidden="true" /> {t.enabled ? 'Disable' : 'Enable'}</button>
                    <button className="btn btn-ghost danger" aria-label={`Delete ${t.machineName}`} onClick={() => remove(t)}><Trash2 size={13} aria-hidden="true" /></button>
                  </td>
                </tr>))}</tbody>
            </table></div>
          )}
        </Async>
        {loaded && targets.length === 0 && <EmptyState title="No targets yet" hint="Add RHEL-01 (192.168.159.129) and RHEL-02 (192.168.159.128)." />}
      </Card>

      {editing && (
        <Card title={editing === 'new' ? 'Add target' : `Edit ${editing.machineName}`}>
          <form className="form-grid" onSubmit={save}>
            <label className="field"><span>Machine name</span><input required value={form.machineName} onChange={(e) => setForm({ ...form, machineName: e.target.value })} placeholder="RHEL-01" /></label>
            <label className="field"><span>Target IP</span><input required value={form.ipAddress} onChange={(e) => setForm({ ...form, ipAddress: e.target.value })} placeholder="192.168.159.129" inputMode="decimal" /></label>
            <label className="field"><span>SSH username</span><input required value={form.sshUsername} onChange={(e) => setForm({ ...form, sshUsername: e.target.value })} autoComplete="off" /></label>
            <label className="field"><span>SSH port</span><input type="number" min={1} max={65535} value={form.sshPort ?? 22} onChange={(e) => setForm({ ...form, sshPort: Number(e.target.value) })} /></label>
            <label className="check"><input type="checkbox" checked={form.enabled ?? true} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} /> Enabled (collected on schedule)</label>
            <p className="muted small span-all">No password or key is entered here. Credentials come from the backend's local configuration.</p>
            {formError && <div className="form-error span-all" role="alert">{formError}</div>}
            <div className="span-all row-gap">
              <button className="btn btn-primary" type="submit">Save</button>
              <button className="btn btn-ghost" type="button" onClick={() => setEditing(null)}>Cancel</button>
            </div>
          </form>
        </Card>
      )}

      <Card title="Collection configuration">
        <div className="grid cols-3 tight">
          <MetricCard label="Normal interval" value="30 min" tone="info" sub="sequential, one target at a time" />
          <MetricCard label="Maximum tolerance" value="45 min" tone="warn" sub="never exceeded" />
          <MetricCard label="Connections" value="Temporary" tone="ok" sub="opened per collection, then closed" />
        </div>
        <p className="muted small">The interval is set in the backend (COLLECTION_INTERVAL_MINUTES). Offline targets keep their history and last-seen time and are retried at the next cycle.</p>
      </Card>
    </>
  );
}
