import { expect, test } from '@playwright/test';
import { emptyDraft, parseSavedProfile, technologyError, validateProfile, type ProfileDraft } from '../src/profile/model';

const valid: ProfileDraft = {
  role: 'backend', technologies: ['Java'], experienceYears: '0',
  country: 'Spain', city: 'Madrid', workMode: 'remote', maxOnsiteDays: '',
};

test('normalizes whitespace, preserves zero years, and clears remote day limits', () => {
  const result = validateProfile({ ...valid, technologies: ['  Java  ', 'Spring   Boot'], country: ' Spain ', maxOnsiteDays: '4' });
  expect(result).toEqual({ ok: true, profile: {
    role: 'backend', technologies: ['Java', 'Spring Boot'], experienceYears: 0,
    country: 'Spain', city: 'Madrid', workMode: 'remote', maxOnsiteDays: null,
  } });
});

test('rejects missing fields, invalid roles and numeric edge cases', () => {
  expect(validateProfile(emptyDraft()).ok).toBe(false);
  for (const role of ['unknown', '__proto__', 'toString']) {
    expect(validateProfile({ ...valid, role }).ok).toBe(false);
  }
  for (const experienceYears of ['', ' ', '-1', '0.5', '61', '1e1', 'Infinity']) {
    expect(validateProfile({ ...valid, experienceYears }).ok).toBe(false);
  }
  expect(validateProfile({ ...valid, experienceYears: '60' }).ok).toBe(true);
  expect(validateProfile({ ...valid, country: ' '.repeat(3) }).ok).toBe(false);
  expect(validateProfile({ ...valid, city: 'x'.repeat(81) }).ok).toBe(false);
});

test('requires bounded on-site days for both hybrid and on-site modes', () => {
  for (const workMode of ['hybrid', 'onsite']) {
    for (const maxOnsiteDays of ['', '0', '6', '1.5', '-1']) {
      expect(validateProfile({ ...valid, workMode, maxOnsiteDays }).ok).toBe(false);
    }
    for (const maxOnsiteDays of ['1', '5']) {
      expect(validateProfile({ ...valid, workMode, maxOnsiteDays }).ok).toBe(true);
    }
  }
});

test('technology limits reject duplicates, blanks, and excess entries', () => {
  expect(technologyError(' java ', ['Java'])).toContain('already');
  expect(technologyError(' ', [])).toBeTruthy();
  expect(technologyError('x'.repeat(41), [])).toBeTruthy();
  const technologies = Array.from({ length: 20 }, (_, i) => 'Technology ' + i);
  expect(validateProfile({ ...valid, technologies }).ok).toBe(true);
  expect(technologyError('One more', technologies)).toBeTruthy();
  expect(validateProfile({ ...valid, technologies: [...technologies, 'One more'] }).ok).toBe(false);
  expect(validateProfile({ ...valid, technologies: ['Java', ' JAVA '] }).ok).toBe(false);
});

test('saved data requires the known version, shape, and valid values', () => {
  const result = validateProfile(valid);
  if (!result.ok) throw new Error('Invalid fixture');
  const envelope = { version: 1, profile: result.profile };
  expect(parseSavedProfile(JSON.stringify(envelope))).toEqual(result.profile);
  for (const raw of ['{', 'null', '[]', '{}', 'x'.repeat(32_769),
    JSON.stringify({ ...envelope, version: 2 }),
    JSON.stringify({ ...envelope, profile: { ...result.profile, experienceYears: '0' } }),
    JSON.stringify({ ...envelope, profile: { ...result.profile, maxOnsiteDays: 3 } }),
    JSON.stringify({ ...envelope, profile: { ...result.profile, technologies: [null] } }),
  ]) expect(parseSavedProfile(raw)).toBeNull();
});
