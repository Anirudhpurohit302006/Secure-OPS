import { render, screen } from '@testing-library/react';
import StatusBadge from '../components/StatusBadge';

describe('StatusBadge', () => {
  it.each([
    ['ONLINE', 'Online', 'ok'], ['OFFLINE', 'Offline', 'bad'], ['FAILED', 'Failed', 'bad'],
    ['PERMISSIVE', 'Permissive', 'warn'], ['AT_RISK', 'At risk', 'bad'], ['SECURE', 'Secure', 'ok'],
  ])('%s renders text label "%s" with %s tone (not colour alone)', (status, label, tone) => {
    render(<StatusBadge status={status} />);
    const badge = screen.getByTestId('status-badge');
    expect(badge).toHaveTextContent(label);
    expect(badge).toHaveAttribute('data-tone', tone);
  });

  it('falls back to Unknown for missing status', () => {
    render(<StatusBadge status={null} />);
    expect(screen.getByTestId('status-badge')).toHaveTextContent('Unknown');
  });
});
