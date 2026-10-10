export interface ProblemDetails {
  readonly type?: string;
  readonly title?: string;
  readonly status?: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly correlationId?: string;
  readonly [extension: string]: unknown;
}

const isOptional = (value: unknown, type: 'string' | 'number'): boolean =>
  value === undefined || typeof value === type;

export function isProblemDetails(value: unknown): value is ProblemDetails {
  if (value === null || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    isOptional(candidate['type'], 'string') &&
    isOptional(candidate['title'], 'string') &&
    isOptional(candidate['status'], 'number') &&
    isOptional(candidate['detail'], 'string') &&
    (candidate['title'] !== undefined || candidate['status'] !== undefined)
  );
}

/**
 * Normalises any error body to RFC 7807, defaulting `type` to "about:blank" as the RFC specifies.
 */
export function toProblemDetails(
  status: number,
  body: unknown,
): ProblemDetails & { readonly type: string; readonly status: number } {
  if (isProblemDetails(body)) {
    return { ...body, type: body.type ?? 'about:blank', status: body.status ?? status };
  }
  return { type: 'about:blank', status, title: `HTTP ${status}` };
}
