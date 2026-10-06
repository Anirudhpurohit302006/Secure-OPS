export type Tone = 'ok' | 'warn' | 'bad' | 'info' | 'muted';

const MAP: Record<string, { tone: Tone; label: string }> = {
  ONLINE: { tone: 'ok', label: 'Online' }, OFFLINE: { tone: 'bad', label: 'Offline' }, UNKNOWN: { tone: 'muted', label: 'Unknown' },
  RUNNING: { tone: 'ok', label: 'Running' }, STOPPED: { tone: 'warn', label: 'Stopped' }, FAILED: { tone: 'bad', label: 'Failed' },
  SECURE: { tone: 'ok', label: 'Secure' }, WARNING: { tone: 'warn', label: 'Warning' }, AT_RISK: { tone: 'bad', label: 'At risk' },
  ENFORCING: { tone: 'ok', label: 'Enforcing' }, PERMISSIVE: { tone: 'warn', label: 'Permissive' }, DISABLED: { tone: 'bad', label: 'Disabled' },
  ACTIVE: { tone: 'ok', label: 'Active' }, INACTIVE: { tone: 'bad', label: 'Inactive' },
  INFO: { tone: 'info', label: 'Info' }, LOW: { tone: 'info', label: 'Low' }, MEDIUM: { tone: 'warn', label: 'Medium' },
  HIGH: { tone: 'bad', label: 'High' }, CRITICAL: { tone: 'bad', label: 'Critical' },
};

export function statusInfo(value: string | null | undefined): { tone: Tone; label: string } {
  if (!value) return { tone: 'muted', label: 'Unknown' };
  return MAP[value] ?? { tone: 'muted', label: value };
}

export const boolStatus = (v: boolean | null | undefined): string => (v == null ? 'UNKNOWN' : v ? 'ACTIVE' : 'INACTIVE');

/** Utilisation thresholds for gauges. */
export const loadTone = (v: number | null | undefined): Tone =>
  v == null ? 'muted' : v >= 90 ? 'bad' : v >= 75 ? 'warn' : 'ok';

export const EVENT_LABELS: Record<string, string> = {
  SSH_LOGIN_SUCCESS: 'Successful login', SSH_LOGIN_FAILED: 'Failed login', STARTUP: 'Startup', SHUTDOWN: 'Shutdown',
  REBOOT: 'Reboot', SERVICE_FAILURE: 'Service failure', SERVICE_START: 'Service started', SERVICE_STOP: 'Service stopped',
};
export const eventLabel = (t: string): string => EVENT_LABELS[t] ?? t.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
