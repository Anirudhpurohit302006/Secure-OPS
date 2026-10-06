import type {
  ActionResult, AnalyticsPoint, AuthAnalytics, AuthEvent, DashboardSummary, EventItem, ListeningPort,
  NetworkResponse, RangeKey, SecurityAnalytics, SecurityEvent, SecurityResponse, ServiceAnalytics,
  ServiceHistoryPoint, ServiceStatus, SessionUser, SystemMetric, SystemOverview, Target, TargetId, TargetRequest,
} from '../types';

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message); }
}

/** Fired on any 401 so the app can drop back to the login page. */
export const UNAUTHORIZED_EVENT = 'secureops:unauthorized';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`/api${path}`, {
      credentials: 'include',
      ...init,
      headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the SecureOps backend. Is it running on port 8080?');
  }
  if (res.status === 401 && !path.startsWith('/auth/login')) window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  if (res.status === 204) return undefined as T;
  let body: unknown = null;
  try { body = await res.json(); } catch { /* non-JSON error page */ }
  if (!res.ok) {
    const b = (body ?? {}) as { error?: string; message?: string };
    throw new ApiError(res.status, b.error ?? 'ERROR', b.message ?? `Request failed (${res.status}).`);
  }
  return body as T;
}

const q = (params: Record<string, string | number | undefined>) => {
  const s = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => v !== undefined && s.set(k, String(v)));
  return `?${s.toString()}`;
};

export const api = {
  login: (username: string, password: string) =>
    request<SessionUser>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  session: () => request<SessionUser>('/auth/session'),

  targets: () => request<Target[]>('/targets'),
  createTarget: (t: TargetRequest) => request<Target>('/targets', { method: 'POST', body: JSON.stringify(t) }),
  updateTarget: (id: number, t: TargetRequest) => request<Target>(`/targets/${id}`, { method: 'PUT', body: JSON.stringify(t) }),
  deleteTarget: (id: number) => request<void>(`/targets/${id}`, { method: 'DELETE' }),
  testTarget: (id: number) => request<ActionResult>(`/targets/${id}/test`, { method: 'POST' }),
  collectTarget: (id: number) => request<ActionResult>(`/targets/${id}/collect`, { method: 'POST' }),

  systems: () => request<SystemOverview[]>('/systems'),
  system: (id: number) => request<SystemOverview>(`/systems/${id}`),
  systemMetrics: (id: number, range: RangeKey) => request<SystemMetric[]>(`/systems/${id}/metrics${q({ range })}`),
  network: (id: number) => request<NetworkResponse>(`/systems/${id}/network`),
  ports: (id: number) => request<ListeningPort[]>(`/systems/${id}/ports`),
  services: (id: number) => request<ServiceStatus[]>(`/systems/${id}/services`),
  serviceHistory: (id: number, range: RangeKey) => request<ServiceHistoryPoint[]>(`/systems/${id}/services/history${q({ range })}`),
  security: (id: number) => request<SecurityResponse>(`/systems/${id}/security`),
  authEvents: (target: TargetId, range: RangeKey, type?: string) =>
    request<AuthEvent[]>(`/systems/${target}/auth-events${q({ range, type })}`),
  securityEvents: (target: TargetId, range: RangeKey) =>
    request<SecurityEvent[]>(`/systems/${target}/security-events${q({ range })}`),

  dashboardSummary: (target: TargetId) => request<DashboardSummary>(`/dashboard/summary${q({ target })}`),
  dashboardEvents: (target: TargetId, limit = 30) => request<EventItem[]>(`/dashboard/events${q({ target, limit })}`),

  analyticsSeries: (metric: 'cpu' | 'memory' | 'disk' | 'network', target: TargetId, range: RangeKey) =>
    request<AnalyticsPoint[]>(`/analytics/${metric}${q({ target, range })}`),
  analyticsServices: (target: TargetId, range: RangeKey) => request<ServiceAnalytics>(`/analytics/services${q({ target, range })}`),
  analyticsAuth: (target: TargetId, range: RangeKey) => request<AuthAnalytics>(`/analytics/authentication${q({ target, range })}`),
  analyticsSecurity: (target: TargetId, range: RangeKey) => request<SecurityAnalytics>(`/analytics/security${q({ target, range })}`),
};
