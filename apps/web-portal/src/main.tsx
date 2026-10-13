import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WebStorageStateStore } from 'oidc-client-ts';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { I18nextProvider } from 'react-i18next';
import { AuthProvider } from 'react-oidc-context';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { readConfig } from './config';
import { createI18n } from './i18n';
import './index.css';

const config = readConfig(import.meta.env);

// The API client retries reads itself, so queries don't retry on top of it; offlineFirst serves cache when the network drops.
const queryClient = new QueryClient({
  defaultOptions: { queries: { networkMode: 'offlineFirst', retry: false, staleTime: 60_000 } },
});

const root = document.getElementById('root');
if (!root) throw new Error('Missing #root element');

createRoot(root).render(
  <StrictMode>
    <AuthProvider
      authority={config.oidcAuthority}
      client_id={config.oidcClientId}
      redirect_uri={`${window.location.origin}/`}
      post_logout_redirect_uri={`${window.location.origin}/`}
      userStore={new WebStorageStateStore({ store: window.sessionStorage })}
      onSigninCallback={() => window.history.replaceState({}, document.title, window.location.pathname)}
    >
      <I18nextProvider i18n={createI18n()}>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </I18nextProvider>
    </AuthProvider>
  </StrictMode>,
);
