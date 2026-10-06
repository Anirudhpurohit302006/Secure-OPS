import TimeSeriesChart from './TimeSeriesChart';
import type { AnalyticsPoint } from '../types';

export default function MemoryChart({ points, height }: { points: AnalyticsPoint[]; height?: number }) {
  return <TimeSeriesChart points={points} name="RAM" height={height} />;
}
