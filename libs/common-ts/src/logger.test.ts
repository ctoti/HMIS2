import { consoleSink, createLogger, type LogEntry } from './logger';
import { MASK } from './redact';

const fixedNow = () => new Date('2026-10-10T00:00:00.000Z');

function capture() {
  const entries: LogEntry[] = [];
  return { entries, sink: (entry: LogEntry) => entries.push(entry) };
}

describe('createLogger', () => {
  it('writes structured, redacted entries', () => {
    const { entries, sink } = capture();
    const logger = createLogger({ sink, now: fixedNow });

    logger.info('sent to a.b@pha.gov.pg', { email: 'a.b@pha.gov.pg', facilityCode: 'PMGH' });

    expect(entries).toEqual([
      {
        timestamp: '2026-10-10T00:00:00.000Z',
        level: 'info',
        message: `sent to ${MASK}`,
        context: { email: MASK, facilityCode: 'PMGH' },
      },
    ]);
  });

  it('omits context when none is given', () => {
    const { entries, sink } = capture();

    createLogger({ sink, now: fixedNow }).warn('slow sync');

    expect(entries[0]).not.toHaveProperty('context');
  });

  it('drops entries below the minimum level', () => {
    const { entries, sink } = capture();
    const logger = createLogger({ sink, minLevel: 'warn' });

    logger.debug('d');
    logger.info('i');
    logger.warn('w');
    logger.error('e');

    expect(entries.map((e) => e.level)).toEqual(['warn', 'error']);
  });
});

describe('consoleSink', () => {
  afterEach(() => vi.restoreAllMocks());

  it.each([
    ['error', 'error'],
    ['warn', 'warn'],
    ['info', 'info'],
    ['debug', 'info'],
  ] as const)('routes %s to console.%s', (level, method) => {
    const spy = vi.spyOn(console, method).mockImplementation(() => undefined);

    consoleSink({ timestamp: 't', level, message: 'm' });

    expect(spy).toHaveBeenCalledWith(JSON.stringify({ timestamp: 't', level, message: 'm' }));
  });
});
