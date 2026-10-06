import { bucketKey, fmtBytes, fmtUptime, parseTs, pivotByMachine, timeAgo } from '../utils/format';

describe('format utils', () => {
  it('treats backend timestamps as UTC', () => {
    expect(parseTs('2026-10-04T12:00:00').toISOString()).toBe('2026-10-04T12:00:00.000Z');
    expect(parseTs('2026-10-04T12:00:00Z').toISOString()).toBe('2026-10-04T12:00:00.000Z');
  });
  it('formats bytes, uptime and relative time', () => {
    expect(fmtBytes(1536)).toBe('1.5 KB');
    expect(fmtBytes(null)).toBe('—');
    expect(fmtUptime(90061)).toBe('1d 1h');
    expect(fmtUptime(125)).toBe('2m');
    const now = parseTs('2026-10-04T12:00:00').getTime();
    expect(timeAgo('2026-10-04T11:30:00', now)).toBe('30 min ago');
    expect(timeAgo(null)).toBe('never');
  });
  it('pivots per-machine points collected seconds apart into one row', () => {
    const pts = [
      { timestamp: '2026-10-04T12:00:05', machineName: 'RHEL-01', v: 10 },
      { timestamp: '2026-10-04T12:00:48', machineName: 'RHEL-02', v: 20 },
    ];
    const { rows, series } = pivotByMachine(pts, (p) => p.v);
    expect(series).toEqual(['RHEL-01', 'RHEL-02']);
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ t: bucketKey('2026-10-04T12:00:05'), 'RHEL-01': 10, 'RHEL-02': 20 });
  });
});
