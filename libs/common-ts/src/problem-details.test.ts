import { isProblemDetails, toProblemDetails } from './problem-details';

describe('isProblemDetails', () => {
  it('accepts an RFC 7807 body', () => {
    expect(isProblemDetails({ type: 'urn:pha:problem:not-found', title: 'Not found', status: 404 })).toBe(true);
  });

  it.each([null, 'error', 42, {}, { status: '404' }, { title: 7 }])('rejects %j', (value) => {
    expect(isProblemDetails(value)).toBe(false);
  });
});

describe('toProblemDetails', () => {
  it('keeps a valid body and fills defaults', () => {
    expect(toProblemDetails(404, { title: 'Not found', correlationId: 'c-1' })).toEqual({
      type: 'about:blank',
      title: 'Not found',
      status: 404,
      correlationId: 'c-1',
    });
  });

  it('keeps the server status and type when present', () => {
    expect(toProblemDetails(500, { type: 'urn:x', status: 409, title: 'Conflict' })).toMatchObject({
      type: 'urn:x',
      status: 409,
    });
  });

  it('wraps a non-problem body', () => {
    expect(toProblemDetails(502, '<html>Bad gateway</html>')).toEqual({
      type: 'about:blank',
      status: 502,
      title: 'HTTP 502',
    });
  });
});
