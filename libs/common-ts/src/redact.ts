export const MASK = '[REDACTED]';

const SENSITIVE_KEY_SUFFIXES = [
  'firstname', 'lastname', 'givenname', 'familyname', 'fullname', 'surname',
  'patientname', 'staffname', 'dob', 'dateofbirth', 'birthdate',
  'mrn', 'medicalrecordnumber', 'nationalid', 'email', 'phone',
  'phonenumber', 'mobile', 'address', 'contactnumber',
] as const;

// Bounded to RFC 5321 lengths so matching stays linear on hostile input (CodeQL polynomial ReDoS).
const EMAIL = /[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,63}/g;

// PNG numbers: any +675 number, or an 8-digit mobile starting with 7.
const PHONE = /(?<![\d+])(?:\+675[\s-]?\d{3,4}[\s-]?\d{4}|7\d{3}[\s-]?\d{4})(?!\d)/g;

export function isSensitiveKey(key: string): boolean {
  const normalized = key.toLowerCase().replace(/[^a-z0-9]/g, '');
  return normalized === 'name' || SENSITIVE_KEY_SUFFIXES.some((suffix) => normalized.endsWith(suffix));
}

export function redactText(text: string): string {
  return text.replace(EMAIL, MASK).replace(PHONE, MASK);
}

export function redactValue(value: unknown, depth = 0): unknown {
  // Bounds recursion on cyclic or pathological input.
  if (depth > 10) {
    return MASK;
  }
  if (typeof value === 'string') {
    return redactText(value);
  }
  if (Array.isArray(value)) {
    return value.map((item) => redactValue(item, depth + 1));
  }
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        isSensitiveKey(key) ? MASK : redactValue(item, depth + 1),
      ]),
    );
  }
  return value;
}
