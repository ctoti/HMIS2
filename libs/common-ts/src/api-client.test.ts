import { ApiError, createApiClient, type ApiClientOptions } from './api-client';
import { CORRELATION_ID_HEADER } from './correlation';

const json = (status: number, body: unknown) =>
  new Response(body === undefined ? '' : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

const asRecord = (data: unknown) => data as Record<string, unknown>;

function setup(responses: Array<Response | Error>, overrides: Partial<ApiClientOptions> = {}) {
  const calls: Array<{ url: string; init: RequestInit }> = [];
  const sleeps: number[] = [];
  const fetchImpl = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), init: init ?? {} });
    const next = responses.shift();
    if (next instanceof Error) throw next;
    if (!next) throw new Error('no response queued');
    return next;
  });
  const client = createApiClient({
    baseUrl: 'https://gw.example/api/v1/',
    getAccessToken: () => 'token-123',
    fetchImpl,
    sleep: async (ms) => { sleeps.push(ms); },
    ...overrides,
  });
  return { client, calls, sleeps };
}

const headersOf = (init: RequestInit) => init.headers as Record<string, string>;

function only<T>(items: readonly T[]): T {
  const [item] = items;
  if (item === undefined) throw new Error('expected a call');
  return item;
}

describe('createApiClient', () => {
  it('sends auth, correlation and JSON headers and parses the body', async () => {
    const { client, calls } = setup([json(200, { id: 'a1' })]);

    const result = await client.request('GET', '/leave-requests/a1', { parse: asRecord });

    expect(result).toEqual({ id: 'a1' });
    expect(calls[0]?.url).toBe('https://gw.example/api/v1/leave-requests/a1');
    const headers = headersOf(only(calls).init);
    expect(headers['Authorization']).toBe('Bearer token-123');
    expect(headers[CORRELATION_ID_HEADER]).toMatch(/^[A-Za-z0-9-]{8,64}$/);
    expect(headers['Content-Type']).toBeUndefined();
  });

  it('sends a JSON body and Idempotency-Key on writes', async () => {
    const { client, calls } = setup([json(201, { id: 'n1' })]);

    await client.request('POST', '/leave-requests', { parse: asRecord, body: { days: 3 }, idempotencyKey: 'k-1' });

    const { init } = only(calls);
    expect(init.body).toBe('{"days":3}');
    expect(headersOf(init)['Content-Type']).toBe('application/json');
    expect(headersOf(init)['Idempotency-Key']).toBe('k-1');
  });

  it('omits Authorization when signed out and handles empty bodies', async () => {
    const { client, calls } = setup([new Response(null, { status: 204 })], { getAccessToken: () => undefined });

    await expect(client.request('DELETE', '/x', { parse: (d) => d })).resolves.toBeUndefined();
    expect(headersOf(only(calls).init)['Authorization']).toBeUndefined();
  });

  it('turns an RFC 7807 response into an ApiError', async () => {
    const { client } = setup([json(404, { type: 'urn:pha:problem:not-found', title: 'Resource not found', status: 404 })]);

    const error = await client.request('GET', '/x', { parse: asRecord }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).problem).toMatchObject({ type: 'urn:pha:problem:not-found', status: 404 });
  });

  it('wraps a non-JSON error body', async () => {
    const { client } = setup([new Response('<html>Bad gateway</html>', { status: 502 })], { maxRetries: 0 });

    const error = (await client.request('GET', '/x', { parse: asRecord }).catch((e: unknown) => e)) as ApiError;

    expect(error.problem).toEqual({ type: 'about:blank', status: 502, title: 'HTTP 502' });
  });

  it('retries reads on 5xx and network errors with exponential backoff', async () => {
    const { client, sleeps } = setup([json(503, undefined), new TypeError('offline'), json(200, { ok: true })]);

    await expect(client.request('GET', '/x', { parse: asRecord })).resolves.toEqual({ ok: true });
    expect(sleeps).toEqual([300, 600]);
  });

  it('gives up after maxRetries with a network ApiError', async () => {
    const { client } = setup([new TypeError('offline'), new TypeError('offline'), new TypeError('offline')]);

    const error = (await client.request('GET', '/x', { parse: asRecord }).catch((e: unknown) => e)) as ApiError;

    expect(error.status).toBe(0);
    expect(error.message).toBe('Network unavailable');
  });

  it('does not retry writes without an Idempotency-Key', async () => {
    const { client, calls } = setup([json(500, undefined)]);

    await expect(client.request('POST', '/x', { parse: asRecord, body: {} })).rejects.toBeInstanceOf(ApiError);
    expect(calls).toHaveLength(1);
  });

  it('does not retry client errors', async () => {
    const { client, calls } = setup([json(400, { title: 'Bad request', status: 400 })]);

    await expect(client.request('GET', '/x', { parse: asRecord })).rejects.toBeInstanceOf(ApiError);
    expect(calls).toHaveLength(1);
  });

  it('rethrows aborts without retrying', async () => {
    const controller = new AbortController();
    controller.abort();
    const abort = new DOMException('Aborted', 'AbortError');
    const { client, calls } = setup([abort]);

    await expect(client.request('GET', '/x', { parse: asRecord, signal: controller.signal })).rejects.toBe(abort);
    expect(calls).toHaveLength(1);
  });

  it('uses global fetch and real timers by default', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, { ok: 1 }));
    const client = createApiClient({ baseUrl: 'https://gw.example', getAccessToken: () => undefined });

    await expect(client.request('GET', '/y', { parse: asRecord })).resolves.toEqual({ ok: 1 });
    spy.mockRestore();
  });
});
