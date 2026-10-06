/** Backend timestamps are UTC without zone info; make the browser treat them as UTC. */
export function parseTs(ts: string): Date {
  return new Date(/[zZ]|[+-]\d{2}:?\d{2}$/.test(ts) ? ts : `${ts}Z`);
}

export const fmtDateTime = (ts: string | null | undefined): string =>
  ts ? parseTs(ts).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export const fmtTime = (ts: string): string => parseTs(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
export const fmtDay = (ts: string): string => parseTs(ts).toLocaleDateString([], { month: 'short', day: 'numeric' });

export function timeAgo(ts: string | null | undefined, now: number = Date.now()): string {
  if (!ts) return 'never';
  const s = Math.max(0, Math.round((now - parseTs(ts).getTime()) / 1000));
  if (s < 60) return 'just now';
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.floor(m / 60);
  if (h < 48) return `${h} h ago`;
  return `${Math.floor(h / 24)} d ago`;
}

export function fmtUptime(seconds: number | null | undefined): string {
  if (seconds == null) return '—';
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return d > 0 ? `${d}d ${h}h` : h > 0 ? `${h}h ${m}m` : `${m}m`;
}

export const fmtPct = (v: number | null | undefined): string => (v == null ? '—' : `${v.toFixed(1)}%`);

export function fmtBytes(b: number | null | undefined): string {
  if (b == null) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let v = b;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

export const fmtRate = (bytesPerSec: number | null | undefined): string =>
  bytesPerSec == null ? '—' : `${fmtBytes(bytesPerSec)}/s`;

/** 5-minute buckets so points from different machines (collected seconds apart) share one chart row. */
export const bucketKey = (ts: string): number => Math.floor(parseTs(ts).getTime() / 300000) * 300000;

export interface Row { t: number; [series: string]: number | null }

/** Pivot AnalyticsPoint-like rows into one row per time bucket, one column per machine. */
export function pivotByMachine<T extends { timestamp: string; machineName: string }>(
  points: T[], pick: (p: T) => number | null,
): { rows: Row[]; series: string[] } {
  const series = Array.from(new Set(points.map((p) => p.machineName))).sort();
  const map = new Map<number, Row>();
  for (const p of points) {
    const t = bucketKey(p.timestamp);
    const row = map.get(t) ?? { t };
    row[p.machineName] = pick(p);
    map.set(t, row);
  }
  return { rows: Array.from(map.values()).sort((a, b) => a.t - b.t), series };
}
