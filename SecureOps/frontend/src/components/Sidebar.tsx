import { BarChart3, Home, Server, Settings, ShieldAlert, X } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const ITEMS = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/systems', label: 'Systems', icon: Server },
  { to: '/security', label: 'Security', icon: ShieldAlert },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/settings', label: 'Settings', icon: Settings },
];

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <>
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Primary">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">S</span>
          <div><strong>SECUREOPS</strong><small>Infrastructure • Security • Analytics</small></div>
          <button className="icon-btn only-mobile" onClick={onClose} aria-label="Close navigation"><X size={18} /></button>
        </div>
        <nav>
          {ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} onClick={onClose} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <Icon size={17} aria-hidden="true" /> {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot muted">SecureOps v1.0</div>
      </aside>
      {open && <div className="scrim" onClick={onClose} aria-hidden="true" />}
    </>
  );
}
