import type { CSSProperties } from 'react';
import { fmtDateTime, fmtDay, fmtTime } from '../utils/format';

export const SERIES_COLORS = ['#4c9aff', '#2dd4bf', '#a78bfa', '#94a3b8'];
export const AXIS = { stroke: '#8b95a7', fontSize: 11 };
export const GRID = '#252c38';

export const tooltipStyle: CSSProperties = {
  background: '#171c25', border: '1px solid #2e3746', borderRadius: 8, color: '#e6eaf2', fontSize: 12,
};

/** X-axis label: time of day for short ranges, date for long ones. */
export function tickFormatter(spanMs: number) {
  return (t: number) => (spanMs <= 36 * 3600 * 1000 ? fmtTime(new Date(t).toISOString()) : fmtDay(new Date(t).toISOString()));
}

export const tooltipLabel = (t: number | string) =>
  typeof t === 'number' ? fmtDateTime(new Date(t).toISOString()) : String(t);
