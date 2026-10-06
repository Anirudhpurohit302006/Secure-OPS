import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import TopBar from '../components/TopBar';

export default function AppLayout() {
  const [open, setOpen] = useState(false);
  return (
    <div className="shell">
      <a className="skip-link" href="#main">Skip to content</a>
      <Sidebar open={open} onClose={() => setOpen(false)} />
      <div className="shell-main">
        <TopBar onMenu={() => setOpen(true)} />
        <main id="main" className="content"><Outlet /></main>
      </div>
    </div>
  );
}
