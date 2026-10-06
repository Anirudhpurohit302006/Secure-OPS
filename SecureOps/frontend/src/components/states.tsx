import { AlertCircle, Inbox, RefreshCw } from 'lucide-react';
import type { ReactNode } from 'react';
import type { ApiError } from '../services/api';

export function LoadingState({ lines = 3, label = 'Loading' }: { lines?: number; label?: string }) {
  return (
    <div className="skeleton-wrap" role="status" aria-live="polite" aria-label={label}>
      {Array.from({ length: lines }).map((_, i) => <div key={i} className="skeleton" style={{ width: `${100 - i * 12}%` }} />)}
    </div>
  );
}

export function EmptyState({ title = 'Waiting for first collection', hint, children }: { title?: string; hint?: string; children?: ReactNode }) {
  return (
    <div className="state state-empty">
      <Inbox size={26} aria-hidden="true" />
      <strong>{title}</strong>
      {hint && <span>{hint}</span>}
      {children}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: ApiError | Error | string; onRetry?: () => void }) {
  const message = typeof error === 'string' ? error : error.message;
  return (
    <div className="state state-error" role="alert">
      <AlertCircle size={26} aria-hidden="true" />
      <strong>Something went wrong</strong>
      <span>{message}</span>
      {onRetry && <button className="btn btn-ghost" onClick={onRetry}><RefreshCw size={14} aria-hidden="true" /> Retry</button>}
    </div>
  );
}

/** Standard handling for a useApi result: loading -> error -> empty -> content. */
export function Async<T>({ state, empty, children, lines }: {
  state: { data: T | null; error: ApiError | null; loading: boolean; reload: () => void };
  empty?: (d: T) => boolean; children: (d: T) => ReactNode; lines?: number;
}) {
  if (state.error && !state.data) return <ErrorState error={state.error} onRetry={state.reload} />;
  if (state.loading && !state.data) return <LoadingState lines={lines} />;
  if (state.data == null) return <EmptyState />;
  if (empty?.(state.data)) return <EmptyState hint="Data appears after the first successful collection." />;
  return <>{children(state.data)}</>;
}
