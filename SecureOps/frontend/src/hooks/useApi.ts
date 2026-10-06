import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../services/api';

export interface ApiState<T> { data: T | null; error: ApiError | null; loading: boolean; reload: () => void }

/**
 * Fetches on mount and whenever `deps` change. Stale responses are ignored.
 * refreshMs re-reads from the SecureOps backend (PostgreSQL) - it never triggers an SSH collection.
 */
export function useApi<T>(fn: () => Promise<T>, deps: unknown[], refreshMs?: number): ApiState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(true);
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const seq = useRef(0);

  const run = useCallback((silent: boolean) => {
    const id = ++seq.current;
    if (!silent) setLoading(true);
    fnRef.current()
      .then((d) => { if (id === seq.current) { setData(d); setError(null); } })
      .catch((e) => { if (id === seq.current) setError(e instanceof ApiError ? e : new ApiError(0, 'ERROR', 'Unexpected error.')); })
      .finally(() => { if (id === seq.current) setLoading(false); });
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { setData(null); run(false); return () => { seq.current++; }; }, deps);
  useEffect(() => {
    if (!refreshMs) return;
    const t = setInterval(() => run(true), refreshMs);
    return () => clearInterval(t);
  }, [refreshMs, run]);

  return { data, error, loading, reload: () => run(false) };
}
