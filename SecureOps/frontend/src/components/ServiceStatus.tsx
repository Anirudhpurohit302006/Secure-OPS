import StatusBadge from './StatusBadge';
import type { ServiceStatus as Svc } from '../types';

const NAMES: Record<string, string> = { sshd: 'SSH', firewalld: 'firewalld', httpd: 'HTTPD' };

export default function ServiceStatus({ services }: { services: Svc[] }) {
  if (services.length === 0) return <p className="muted">No service data yet.</p>;
  return (
    <ul className="service-list">
      {services.map((s) => (
        <li key={s.name}>
          <code>{NAMES[s.name] ?? s.name}</code>
          <StatusBadge status={s.status} size="sm" />
        </li>
      ))}
    </ul>
  );
}
