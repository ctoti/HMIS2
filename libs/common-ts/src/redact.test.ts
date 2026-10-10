import { MASK, isSensitiveKey, redactText, redactValue } from './redact';

describe('isSensitiveKey', () => {
  it.each(['name', 'firstName', 'last_name', 'patientName', 'dob', 'MRN', 'nationalId', 'email', 'homeAddress'])(
    'flags %s',
    (key) => expect(isSensitiveKey(key)).toBe(true),
  );

  it.each(['status', 'facilityCode', 'leaveType', 'fileName', 'id'])('ignores %s', (key) =>
    expect(isSensitiveKey(key)).toBe(false),
  );
});

describe('redactText', () => {
  it('masks emails and PNG phone numbers', () => {
    expect(redactText('mail a.b@pha.gov.pg or call +675 7123 4567 / 7123-4567')).toBe(
      `mail ${MASK} or call ${MASK} / ${MASK}`,
    );
  });

  it('keeps ordinary numbers', () => {
    expect(redactText('processed 1234567 rows')).toBe('processed 1234567 rows');
  });
});

describe('redactValue', () => {
  it('masks sensitive keys at any depth and redacts free text', () => {
    const input = {
      facilityCode: 'PMGH',
      patient: { patientName: 'Mary Kila', mrn: 'PMGH-00123' },
      notes: ['contact nurse.kila@pha.gov.pg'],
      days: 3,
      approved: null,
    };

    expect(redactValue(input)).toEqual({
      facilityCode: 'PMGH',
      patient: { patientName: MASK, mrn: MASK },
      notes: [`contact ${MASK}`],
      days: 3,
      approved: null,
    });
  });

  it('stops at excessive depth', () => {
    type Nested = { child?: Nested };
    const root: Nested = {};
    let node = root;
    for (let i = 0; i < 20; i += 1) {
      node.child = {};
      node = node.child;
    }

    expect(JSON.stringify(redactValue(root))).toContain(MASK);
  });
});
