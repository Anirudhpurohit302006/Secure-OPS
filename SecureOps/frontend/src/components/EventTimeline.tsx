import { KeyRound, Power, ServerCrash, ShieldAlert, UserX } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { eventLabel, statusInfo } from '../utils/status';
import { fmtDateTime } from '../utils/format';
import type { EventItem } from '../types';
import { EmptyState } from './states';

function iconFor(e: EventItem) {
  if (e.type === 'SSH_LOGIN_SUCCESS') return KeyRound;
  if (e.type === 'SSH_LOGIN_FAILED') return UserX;
  if (e.source === 'SECURITY') return ShieldAlert;
  if (e.type === 'SERVICE_FAILURE') return ServerCrash;
  return Power;
}

export default function EventTimeline({ events }: { events: EventItem[] }) {
  if (events.length === 0) return <EmptyState title="No events yet" hint="Events appear after the first collection." />;
  return (
    <ol className="timeline">
      {events.map((e) => {
        const Icon = iconFor(e);
        return (
          <li key={e.id} className={`tl-item tone-${statusInfo(e.severity).tone}`}>
            <span className="tl-icon"><Icon size={15} aria-hidden="true" /></span>
            <div className="tl-body">
              <div className="tl-title"><strong>{eventLabel(e.type)}</strong><StatusBadge status={e.severity} size="sm" /></div>
              <div className="tl-desc">{e.description}</div>
              <div className="tl-meta muted">{e.machineName} · {fmtDateTime(e.timestamp)}</div>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
