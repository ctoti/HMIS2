import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { fakeAuth, renderApp, signedInAs } from './test/render';
import { App } from './App';
import { useUiStore } from './state/ui';

const signedOut = () => {
  fakeAuth.current = { ...fakeAuth.current, isLoading: false, isAuthenticated: false, user: undefined, error: undefined };
};

describe('App', () => {
  beforeEach(() => {
    signedOut();
    useUiStore.setState({ navOpen: false });
  });

  it('shows a status while the session is checked', () => {
    fakeAuth.current = { ...fakeAuth.current, isLoading: true };
    renderApp(<App />);
    expect(screen.getByRole('status')).toHaveTextContent('Checking your sign-in');
  });

  it('asks signed-out users to sign in and starts the redirect', async () => {
    const signinRedirect = vi.fn(async () => undefined);
    fakeAuth.current = { ...fakeAuth.current, signinRedirect };
    renderApp(<App />);

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByRole('heading', { name: 'Sign in to continue' })).toBeInTheDocument();
    expect(signinRedirect).toHaveBeenCalledOnce();
  });

  it('explains a failed sign-in', () => {
    fakeAuth.current = { ...fakeAuth.current, error: new Error('invalid_grant') };
    renderApp(<App />);
    expect(screen.getByRole('alert')).toHaveTextContent('Sign-in failed: invalid_grant');
  });

  it('shows the user their roles, facilities and permitted modules', () => {
    signedInAs(['hr-officer', 'staff'], ['/facilities/PMGH']);
    renderApp(<App />);

    expect(screen.getByRole('heading', { name: 'Welcome' })).toBeInTheDocument();
    expect(screen.getByText('HR officer')).toBeInTheDocument();
    expect(screen.getByText('PMGH')).toBeInTheDocument();
    const modules = within(screen.getByRole('region', { name: 'Modules' }));
    expect(modules.getByRole('link', { name: /Human resources/ })).toBeInTheDocument();
    expect(modules.queryByRole('link', { name: /Governance/ })).not.toBeInTheDocument();
  });

  it('says when no facility is assigned', () => {
    signedInAs(['staff']);
    renderApp(<App />);
    expect(screen.getByText('No facility assigned yet')).toBeInTheDocument();
  });

  it('opens a permitted module and blocks one without the role', async () => {
    signedInAs(['hr-officer']);
    renderApp(<App />);
    const nav = within(screen.getByRole('navigation', { name: 'Main' }));

    await userEvent.click(nav.getByRole('link', { name: 'Human resources' }));
    expect(screen.getByRole('heading', { name: 'Human resources is on the way' })).toBeInTheDocument();

    await userEvent.click(nav.getByRole('link', { name: 'Governance' }));
    expect(screen.getByRole('heading', { name: "You don't have access to Governance" })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown routes', () => {
    signedInAs(['staff']);
    renderApp(<App />, { route: '/nowhere' });
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  });

  it('switches to Tok Pisin', async () => {
    signedInAs(['staff']);
    renderApp(<App />);

    await userEvent.selectOptions(screen.getByLabelText('Language'), 'tpi');

    expect(screen.getByRole('heading', { name: 'Welkam' })).toBeInTheDocument();
    expect(localStorage.getItem('pha.language')).toBe('tpi');
  });

  it('toggles the mobile menu and signs out', async () => {
    const signoutRedirect = vi.fn(async () => undefined);
    signedInAs(['staff']);
    fakeAuth.current = { ...fakeAuth.current, signoutRedirect };
    renderApp(<App />);

    const toggle = screen.getByRole('button', { name: 'Open menu' });
    await userEvent.click(toggle);
    expect(screen.getByRole('button', { name: 'Close menu' })).toHaveAttribute('aria-expanded', 'true');
    await userEvent.click(screen.getByRole('link', { name: 'Home' }));
    expect(screen.getByRole('button', { name: 'Open menu' })).toHaveAttribute('aria-expanded', 'false');

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(signoutRedirect).toHaveBeenCalledOnce();
  });

  it('warns when the device goes offline', () => {
    signedInAs(['staff']);
    const onLine = vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    renderApp(<App />);
    expect(screen.queryByText(/You're offline/)).not.toBeInTheDocument();

    onLine.mockReturnValue(false);
    act(() => { window.dispatchEvent(new Event('offline')); });
    expect(screen.getByText(/You're offline/)).toBeInTheDocument();
    onLine.mockRestore();
  });
});
