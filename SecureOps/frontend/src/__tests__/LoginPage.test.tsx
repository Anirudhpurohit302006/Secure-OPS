import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import LoginPage from '../pages/LoginPage';
import { AuthProvider } from '../hooks/AuthContext';
import { ApiError, api } from '../services/api';

describe('LoginPage', () => {
  beforeEach(() => { vi.spyOn(api, 'session').mockRejectedValue(new ApiError(401, 'UNAUTHENTICATED', 'Sign in required.')); });
  afterEach(() => vi.restoreAllMocks());

  const renderLogin = () => render(<MemoryRouter><AuthProvider><LoginPage /></AuthProvider></MemoryRouter>);

  it('shows the SecureOps identity and a labelled form', () => {
    renderLogin();
    expect(screen.getByText('SECUREOPS')).toBeInTheDocument();
    expect(screen.getByText('Infrastructure • Security • Analytics')).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByText('Version 1.0')).toBeInTheDocument();
  });

  it('displays the server message on bad credentials', async () => {
    vi.spyOn(api, 'login').mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'Invalid username or password.'));
    renderLogin();
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid username or password.'));
  });
});
