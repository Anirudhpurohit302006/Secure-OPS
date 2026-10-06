import { LogOut, Menu, User } from 'lucide-react';
import { useAuth } from '../hooks/AuthContext';
import { useTargets } from '../hooks/TargetContext';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';
import StatusBadge from './StatusBadge';
import TargetSelector from './TargetSelector';

export default function TopBar({ onMenu }: { onMenu: () => void }) {
  const { user, logout } = useAuth();
  const { selected, targets } = useTargets();
  const summary = useApi(() => api.dashboardSummary(selected), [selected], 60000);
  const label = selected === 'all' ? 'All systems' : targets.find((t) => String(t.id) === selected)?.machineName ?? '…';
  return (
    <header className="topbar">
      <button className="icon-btn only-mobile" onClick={onMenu} aria-label="Open navigation"><Menu size={20} /></button>
      <span className="topbar-brand">SECUREOPS</span>
      <TargetSelector />
      <span className="topbar-selected muted">Viewing: {label}</span>
      <div className="spacer" />
      <div className="topbar-status" aria-label="Overall system status">
        <span className="muted hide-sm">Overall</span>
        <StatusBadge status={summary.data?.overallSecurity ?? 'UNKNOWN'} />
      </div>
      <span className="user-chip"><User size={14} aria-hidden="true" /> {user?.username}</span>
      <button className="btn btn-ghost" onClick={() => void logout()}><LogOut size={14} aria-hidden="true" /> <span className="hide-sm">Logout</span></button>
    </header>
  );
}
