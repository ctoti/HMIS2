export const CORRELATION_ID_HEADER = 'X-Correlation-Id';

export function createCorrelationId(): string {
  return crypto.randomUUID();
}
