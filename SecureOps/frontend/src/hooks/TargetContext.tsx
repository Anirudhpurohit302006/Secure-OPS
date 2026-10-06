import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from '../services/api';
import type { Target, TargetId } from '../types';

interface TargetValue {
  targets: Target[]; selected: TargetId; setSelected: (t: TargetId) => void; refresh: () => void; loaded: boolean;
}
const Ctx = createContext<TargetValue | null>(null);

export function TargetProvider({ children }: { children: ReactNode }) {
  const [targets, setTargets] = useState<Target[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [selected, setSelectedState] = useState<TargetId>(() => localStorage.getItem('secureops.target') ?? 'all');

  const refresh = useCallback(() => {
    api.targets().then((t) => { setTargets(t); setLoaded(true); }).catch(() => setLoaded(true));
  }, []);
  useEffect(refresh, [refresh]);

  // If the selected target was deleted, fall back to ALL.
  useEffect(() => {
    if (loaded && selected !== 'all' && !targets.some((t) => String(t.id) === selected)) setSelectedState('all');
  }, [loaded, targets, selected]);

  const setSelected = useCallback((t: TargetId) => {
    setSelectedState(t);
    try { localStorage.setItem('secureops.target', t); } catch { /* storage unavailable */ }
  }, []);

  const value = useMemo(() => ({ targets, selected, setSelected, refresh, loaded }), [targets, selected, setSelected, refresh, loaded]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useTargets(): TargetValue {
  const v = useContext(Ctx);
  if (!v) throw new Error('useTargets outside TargetProvider');
  return v;
}
