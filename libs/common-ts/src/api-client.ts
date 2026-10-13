import { CORRELATION_ID_HEADER, createCorrelationId } from './correlation';
import { toProblemDetails, type ProblemDetails } from './problem-details';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetails & { readonly type: string; readonly status: number };

  constructor(status: number, body: unknown) {
    const problem = toProblemDetails(status, body);
    super(problem.title ?? `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

export interface ApiClientOptions {
  readonly baseUrl: string;
  readonly getAccessToken: () => string | undefined;
  readonly fetchImpl?: typeof fetch;
  readonly maxRetries?: number;
  readonly retryBaseMs?: number;
  readonly sleep?: (ms: number) => Promise<void>;
}

export interface RequestOptions<T> {
  readonly parse: (data: unknown) => T;
  readonly body?: unknown;
  readonly idempotencyKey?: string;
  readonly signal?: AbortSignal;
}

export interface ApiClient {
  request<T>(method: HttpMethod, path: string, options: RequestOptions<T>): Promise<T>;
}

const NETWORK_ERROR_STATUS = 0;
const defaultSleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

// Only reads are retried automatically; writes need an Idempotency-Key to be safe to repeat.
const isRetryable = (method: HttpMethod, status: number, idempotencyKey?: string): boolean =>
  (method === 'GET' || idempotencyKey !== undefined) && (status === NETWORK_ERROR_STATUS || status >= 500);

async function readBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text === '') {
    return undefined;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

export function createApiClient(options: ApiClientOptions): ApiClient {
  const {
    baseUrl,
    getAccessToken,
    fetchImpl = (input, init) => fetch(input, init),
    maxRetries = 2,
    retryBaseMs = 300,
    sleep = defaultSleep,
  } = options;

  const send = async (method: HttpMethod, path: string, body: unknown, idempotencyKey?: string, signal?: AbortSignal) => {
    const headers: Record<string, string> = { Accept: 'application/json', [CORRELATION_ID_HEADER]: createCorrelationId() };
    const token = getAccessToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
    const init: RequestInit = { method, headers };
    if (body !== undefined) init.body = JSON.stringify(body);
    if (signal) init.signal = signal;
    return fetchImpl(`${baseUrl.replace(/\/$/, '')}${path}`, init);
  };

  return {
    async request<T>(method: HttpMethod, path: string, { parse, body, idempotencyKey, signal }: RequestOptions<T>) {
      for (let attempt = 0; ; attempt += 1) {
        let status = NETWORK_ERROR_STATUS;
        let payload: unknown;
        try {
          const response = await send(method, path, body, idempotencyKey, signal);
          status = response.status;
          payload = await readBody(response);
          if (response.ok) return parse(payload);
        } catch (error) {
          if (signal?.aborted) throw error;
          payload = { title: 'Network unavailable' };
        }
        if (attempt >= maxRetries || !isRetryable(method, status, idempotencyKey)) {
          throw new ApiError(status, payload);
        }
        await sleep(retryBaseMs * 2 ** attempt);
      }
    },
  };
}
