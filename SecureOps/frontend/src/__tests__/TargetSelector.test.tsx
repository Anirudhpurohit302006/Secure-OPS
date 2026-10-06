import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import TargetSelector from '../components/TargetSelector';
import { TargetProvider } from '../hooks/TargetContext';
import { api } from '../services/api';
import type { Target } from '../types';

const t = (id: number, name: string): Target => ({ id, machineName: name, hostname: null, ipAddress: '10.0.0.' + id, sshUsername: 'ani', sshPort: 22, enabled: true, status: 'ONLINE', lastSeen: null });

describe('TargetSelector', () => {
  beforeEach(() => { localStorage.clear(); vi.spyOn(api, 'targets').mockResolvedValue([t(1, 'RHEL-01'), t(2, 'RHEL-02')]); });
  afterEach(() => vi.restoreAllMocks());

  it('offers ALL SYSTEMS plus every target and remembers the selection', async () => {
    render(<TargetProvider><TargetSelector /></TargetProvider>);
    await waitFor(() => expect(screen.getByRole('option', { name: 'RHEL-02' })).toBeInTheDocument());
    expect(screen.getByRole('option', { name: 'ALL SYSTEMS' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Target system'), { target: { value: '2' } });
    expect((screen.getByLabelText('Target system') as HTMLSelectElement).value).toBe('2');
    expect(localStorage.getItem('secureops.target')).toBe('2');
  });
});
