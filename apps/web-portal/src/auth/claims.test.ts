import { tokenWith } from '../test/render';
import { hasAnyRole, sessionFromAccessToken } from './claims';

describe('sessionFromAccessToken', () => {
  it('reads known realm roles and facility codes', () => {
    const token = tokenWith({
      realm_access: { roles: ['hr-officer', 'offline_access', 'staff'] },
      groups: ['/facilities/PMGH', '/facilities/ANGAU', '/other/x', '/facilities/', '/facilities/a/b', 7],
    });

    expect(sessionFromAccessToken(token)).toEqual({ roles: ['hr-officer', 'staff'], facilities: ['PMGH', 'ANGAU'] });
  });

  it.each([
    ['no token', undefined],
    ['not a JWT', 'abc'],
    ['bad base64', 'a.@@@.c'],
    ['non-object payload', `a.${btoa('42')}.c`],
    ['missing claims', tokenWith({ sub: 'u1' })],
    ['wrong claim shapes', tokenWith({ realm_access: 'admin', groups: 'x' })],
  ])('returns an empty session for %s', (_label, token) => {
    expect(sessionFromAccessToken(token)).toEqual({ roles: [], facilities: [] });
  });
});

describe('hasAnyRole', () => {
  it('grants platform admins everything', () => {
    expect(hasAnyRole({ roles: ['platform-admin'], facilities: [] }, ['hr-officer'])).toBe(true);
  });

  it('checks the allowed roles otherwise', () => {
    const session = { roles: ['staff'] as const, facilities: [] };
    expect(hasAnyRole(session, ['hr-officer'])).toBe(false);
    expect(hasAnyRole(session, ['staff'])).toBe(true);
  });
});
