import TimeSeriesChart from './TimeSeriesChart';
import type { AnalyticsPoint } from '../types';

export default function CpuChart({ points, height }: { points: AnalyticsPoint[]; height?: number }) {
  return <TimeSeriesChart points={points} name="CPU" height={height} />;
}
