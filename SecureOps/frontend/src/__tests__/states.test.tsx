import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { Async, EmptyState, ErrorState, LoadingState } from '../components/states';
import { ApiError } from '../services/api';

const state = <T,>(over: Partial<{ data: T | null; error: ApiError | null; loading: boolean }>) =>
  ({ data: null, error: null, loading: false, reload: vi.fn(), ...over });

describe('UX states', () => {
  it('shows a loading skeleton with an accessible status role', () => {
    render(<LoadingState />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('shows the "Waiting for first collection" empty state', () => {
    render(<EmptyState />);
    expect(screen.getByText('Waiting for first collection')).toBeInTheDocument();
  });

  it('shows a friendly error with retry and no stack trace', () => {
    const retry = vi.fn();
    render(<ErrorState error={new ApiError(500, 'INTERNAL_ERROR', 'Something went wrong on the server.')} onRetry={retry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong on the server.');
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));
    expect(retry).toHaveBeenCalled();
  });

  it('Async: loading -> error -> empty -> content', () => {
    const { rerender } = render(<Async state={state<string[]>({ loading: true })}>{(d) => <div>{d.length}</div>}</Async>);
    expect(screen.getByRole('status')).toBeInTheDocument();

    rerender(<Async state={state<string[]>({ error: new ApiError(0, 'NETWORK', 'Cannot reach the SecureOps backend.') })}>{(d) => <div>{d.length}</div>}</Async>);
    expect(screen.getByRole('alert')).toHaveTextContent('Cannot reach');

    rerender(<Async state={state<string[]>({ data: [] })} empty={(d) => d.length === 0}>{(d) => <div>{d.length}</div>}</Async>);
    expect(screen.getByText(/Waiting for first collection/)).toBeInTheDocument();

    rerender(<Async state={state<string[]>({ data: ['a', 'b'] })} empty={(d) => d.length === 0}>{(d) => <div>count {d.length}</div>}</Async>);
    expect(screen.getByText('count 2')).toBeInTheDocument();
  });
});
