export const ROLES = [
  'platform-admin',
  'governance-officer',
  'hr-officer',
  'facilities-officer',
  'projects-officer',
  'staff',
] as const;

export type Role = (typeof ROLES)[number];

export interface Session {
  readonly roles: readonly Role[];
  readonly facilities: readonly string[];
}

const FACILITY_GROUP_PREFIX = '/facilities/';

const isRole = (value: unknown): value is Role => typeof value === 'string' && (ROLES as readonly string[]).includes(value);

function decodePayload(jwt: string): Record<string, unknown> {
  const part = jwt.split('.')[1];
  if (!part) return {};
  try {
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(part.length / 4) * 4, '=');
    const parsed: unknown = JSON.parse(atob(base64));
    return parsed !== null && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}

/**
 * Reads roles and facility scope from the Keycloak access token for display and navigation only.
 * Services re-validate the token; the browser never makes an authorisation decision that matters.
 */
export function sessionFromAccessToken(accessToken: string | undefined): Session {
  if (!accessToken) return { roles: [], facilities: [] };
  const payload = decodePayload(accessToken);
  const realmAccess = payload['realm_access'];
  const rawRoles =
    realmAccess !== null && typeof realmAccess === 'object' ? (realmAccess as Record<string, unknown>)['roles'] : undefined;
  const roles = Array.isArray(rawRoles) ? rawRoles.filter(isRole) : [];
  const groups = Array.isArray(payload['groups']) ? payload['groups'] : [];
  const facilities = groups
    .filter((g): g is string => typeof g === 'string' && g.startsWith(FACILITY_GROUP_PREFIX))
    .map((g) => g.slice(FACILITY_GROUP_PREFIX.length))
    .filter((code) => code.length > 0 && !code.includes('/'));
  return { roles, facilities };
}

export const hasAnyRole = (session: Session, allowed: readonly Role[]): boolean =>
  session.roles.includes('platform-admin') || allowed.some((role) => session.roles.includes(role));
