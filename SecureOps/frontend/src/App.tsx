import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';
import { useAuth } from './hooks/AuthContext';
import { TargetProvider } from './hooks/TargetContext';
import { LoadingState } from './components/states';
import AnalyticsPage from './pages/AnalyticsPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SecurityPage from './pages/SecurityPage';
import SettingsPage from './pages/SettingsPage';
import SystemDetailPage from './pages/SystemDetailPage';
import SystemsPage from './pages/SystemsPage';

function Protected() {
  const { user, checking } = useAuth();
  if (checking) return <div className="boot"><LoadingState lines={3} label="Checking session" /></div>;
  if (!user) return <Navigate to="/login" replace />;
  return <TargetProvider><AppLayout /></TargetProvider>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Protected />}>
        <Route index element={<HomePage />} />
        <Route path="systems" element={<SystemsPage />} />
        <Route path="systems/:id" element={<SystemDetailPage />} />
        <Route path="security" element={<SecurityPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
