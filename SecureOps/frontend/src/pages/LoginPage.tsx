import { useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { LogIn, ShieldCheck } from 'lucide-react';
import { useAuth } from '../hooks/AuthContext';
import { ApiError } from '../services/api';

export default function LoginPage() {
  const { user, login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try { await login(username, password); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Sign-in failed.'); }
    finally { setBusy(false); }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit} aria-labelledby="login-title">
        <div className="login-brand">
          <span className="brand-mark big" aria-hidden="true"><ShieldCheck size={26} /></span>
          <h1 id="login-title">SECUREOPS</h1>
          <p className="muted">Infrastructure • Security • Analytics</p>
        </div>
        <label className="field"><span>Username</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus required />
        </label>
        <label className="field"><span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="btn btn-primary wide" type="submit" disabled={busy}>
          <LogIn size={16} aria-hidden="true" /> {busy ? 'Signing in…' : 'SIGN IN'}
        </button>
        <p className="login-version muted">Version 1.0</p>
      </form>
    </div>
  );
}
