import { createCorrelationId } from './correlation';

describe('createCorrelationId', () => {
  it('returns a UUID accepted by the backend filter', () => {
    expect(createCorrelationId()).toMatch(/^[A-Za-z0-9-]{8,64}$/);
  });
});
