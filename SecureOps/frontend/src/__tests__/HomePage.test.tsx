import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import HomePage from '../pages/HomePage';
import { TargetProvider } from '../hooks/TargetContext';
import { ApiError, api } from '../services/api';
import type { DashboardSummary, SystemOverview } from '../types';

const sys = (id: number, name: string, over: Partial<SystemOverview> = {}): SystemOverview => ({
  id, machineName: name, hostname: name.toLowerCase(), ipAddress: '192.168.159.' + id, enabled: true, status: 'ONLINE',
  lastSeen: '2026-10-04T12:00:00', osName: 'RHEL', osVersion: '9.4', kernelVersion: '5.14', machineUid: 'x', cpuUsage: 12.5,
  memoryUsage: 40, diskUsage: 55, uptimeSeconds: 3600, lastCollected: '2026-10-04T12:00:00', securityStatus: 'SECURE',
  firewallActive: true, selinuxMode: 'ENFORCING', sshActive: true, findingsCount: 0,
  services: [{ name: 'sshd', status: 'RUNNING' }, { name: 'firewalld', status: 'RUNNING' }, { name: 'httpd', status: 'FAILED' }], ...over,
});

const summary: DashboardSummary = {
  totalSystems: 2, onlineSystems: 1, offlineSystems: 1, overallSecurity: 'WARNING', failedLogins24h: 7, successfulLogins24h: 3,
  securityFindings: 2, systems: [sys(1, 'RHEL-01'), sys(2, 'RHEL-02', { status: 'OFFLINE', securityStatus: 'WARNING' })],
};

function renderHome() {
  return render(<MemoryRouter><TargetProvider><HomePage /></TargetProvider></MemoryRouter>);
}

describe('HomePage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(api, 'targets').mockResolvedValue([]);
    vi.spyOn(api, 'analyticsSeries').mockResolvedValue([]);
    vi.spyOn(api, 'dashboardEvents').mockResolvedValue([]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('renders overview, service health and security summary from the API', async () => {
    vi.spyOn(api, 'dashboardSummary').mockResolvedValue(summary);
    renderHome();
    await waitFor(() => expect(screen.getByText('monitored RHEL targets')).toBeInTheDocument());
    expect(screen.getByText('2 open findings')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();           // failed logins
    expect(screen.getAllByText('HTTPD').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Failed').length).toBeGreaterThan(0);   // httpd failed badge
    expect(screen.getAllByText('Enforcing').length).toBeGreaterThan(0);
  });

  it('shows a loading state first, then empty chart states when there is no history', async () => {
    vi.spyOn(api, 'dashboardSummary').mockResolvedValue(summary);
    renderHome();
    expect(screen.getAllByRole('status').length).toBeGreaterThan(0);
    await waitFor(() => expect(screen.getAllByText('Waiting for first collection').length).toBeGreaterThan(0));
  });

  it('shows a user-facing error when the backend is unreachable', async () => {
    vi.spyOn(api, 'dashboardSummary').mockRejectedValue(new ApiError(0, 'NETWORK', 'Cannot reach the SecureOps backend. Is it running on port 8080?'));
    renderHome();
    await waitFor(() => expect(screen.getAllByRole('alert').length).toBeGreaterThan(0));
    expect(screen.getAllByText(/Cannot reach the SecureOps backend/).length).toBeGreaterThan(0);
  });
});
