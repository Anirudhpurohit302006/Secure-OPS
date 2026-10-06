import TimeSeriesChart from './TimeSeriesChart';
import type { AnalyticsPoint } from '../types';

export default function DiskChart({ points, height }: { points: AnalyticsPoint[]; height?: number }) {
  return <TimeSeriesChart points={points} name="Disk" height={height} />;
}
