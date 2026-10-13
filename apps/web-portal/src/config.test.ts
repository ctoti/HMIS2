import { readConfig } from './config';

describe('readConfig', () => {
  it('uses local defaults that match infra/docker', () => {
    expect(readConfig({} as ImportMetaEnv)).toEqual({
      oidcAuthority: 'http://localhost:8180/realms/pha-hmg',
      oidcClientId: 'web-portal',
      apiBaseUrl: 'http://localhost:8080/api/v1',
    });
  });

  it('prefers environment values', () => {
    const env = { VITE_OIDC_AUTHORITY: 'https://id.pha.example/realms/pha-hmg', VITE_OIDC_CLIENT_ID: 'portal', VITE_API_BASE_URL: 'https://api.pha.example/api/v1' } as ImportMetaEnv;
    expect(readConfig(env)).toEqual({ oidcAuthority: env.VITE_OIDC_AUTHORITY, oidcClientId: 'portal', apiBaseUrl: env.VITE_API_BASE_URL });
  });
});
