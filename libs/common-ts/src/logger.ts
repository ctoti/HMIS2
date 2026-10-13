import { redactText, redactValue } from './redact';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export interface LogEntry {
  readonly timestamp: string;
  readonly level: LogLevel;
  readonly message: string;
  readonly context?: unknown;
}

export type LogSink = (entry: LogEntry) => void;

export interface Logger {
  debug(message: string, context?: Record<string, unknown>): void;
  info(message: string, context?: Record<string, unknown>): void;
  warn(message: string, context?: Record<string, unknown>): void;
  error(message: string, context?: Record<string, unknown>): void;
}

export interface LoggerOptions {
  readonly minLevel?: LogLevel;
  readonly sink?: LogSink;
  readonly now?: () => Date;
}

const LEVEL_ORDER: Record<LogLevel, number> = { debug: 0, info: 1, warn: 2, error: 3 };

export const consoleSink: LogSink = (entry) => {
  const line = JSON.stringify(entry);
  if (entry.level === 'error') console.error(line);
  else if (entry.level === 'warn') console.warn(line);
  else console.info(line);
};

export function createLogger(options: LoggerOptions = {}): Logger {
  const { minLevel = 'info', sink = consoleSink, now = () => new Date() } = options;

  const write = (level: LogLevel, message: string, context?: Record<string, unknown>): void => {
    if (LEVEL_ORDER[level] < LEVEL_ORDER[minLevel]) {
      return;
    }
    const base = { timestamp: now().toISOString(), level, message: redactText(message) };
    sink(context === undefined ? base : { ...base, context: redactValue(context) });
  };

  return {
    debug: (message, context) => write('debug', message, context),
    info: (message, context) => write('info', message, context),
    warn: (message, context) => write('warn', message, context),
    error: (message, context) => write('error', message, context),
  };
}
