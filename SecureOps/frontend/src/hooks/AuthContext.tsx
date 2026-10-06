import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, UNAUTHORIZED_EVENT } from '../services/api';
import type { SessionUser } from '../types';

interface AuthValue {
  user: SessionUser | null; checking: boolean;
  login: (u: string, p: string) => Promise<void>; logout: () => Promise<void>;
}
const Ctx = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    api.session().then(setUser).catch(() => setUser(null)).finally(() => setChecking(false));
    const onUnauthorized = () => setUser(null);
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const login = useCallback(async (u: string, p: string) => setUser(await api.login(u, p)), []);
  const logout = useCallback(async () => { try { await api.logout(); } finally { setUser(null); } }, []);
  const value = useMemo(() => ({ user, checking, login, logout }), [user, checking, login, logout]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthValue {
  const v = useContext(Ctx);
  if (!v) throw new Error('useAuth outside AuthProvider');
  return v;
}
