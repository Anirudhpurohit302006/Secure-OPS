import type { ReactNode } from 'react';
import { Async } from './states';
import type { ApiState } from '../hooks/useApi';

export function Card({ title, subtitle, actions, children, className = '' }: {
  title?: string; subtitle?: string; actions?: ReactNode; children: ReactNode; className?: string;
}) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          <div><h2>{title}</h2>{subtitle && <p className="muted">{subtitle}</p>}</div>
          {actions}
        </header>
      )}
      {children}
    </section>
  );
}

/** Card + Async data handling + fixed-height chart area. */
export default function ChartCard<T>({ title, subtitle, state, empty, children, actions, className, height = 240 }: {
  title: string; subtitle?: string; state: ApiState<T>; empty?: (d: T) => boolean;
  children: (d: T) => ReactNode; actions?: ReactNode; className?: string; height?: number;
}) {
  return (
    <Card title={title} subtitle={subtitle} actions={actions} className={className}>
      <div className="chart-area" style={{ minHeight: height }}>
        <Async state={state} empty={empty}>{children}</Async>
      </div>
    </Card>
  );
}
