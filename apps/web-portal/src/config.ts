export interface PortalConfig {
  readonly oidcAuthority: string;
  readonly oidcClientId: string;
  readonly apiBaseUrl: string;
}

const DEFAULTS: PortalConfig = {
  oidcAuthority: 'http://localhost:8180/realms/pha-hmg',
  oidcClientId: 'web-portal',
  apiBaseUrl: 'http://localhost:8080/api/v1',
};

export function readConfig(env: ImportMetaEnv): PortalConfig {
  return {
    oidcAuthority: env.VITE_OIDC_AUTHORITY || DEFAULTS.oidcAuthority,
    oidcClientId: env.VITE_OIDC_CLIENT_ID || DEFAULTS.oidcClientId,
    apiBaseUrl: env.VITE_API_BASE_URL || DEFAULTS.apiBaseUrl,
  };
}
