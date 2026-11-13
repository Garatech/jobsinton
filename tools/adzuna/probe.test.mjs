import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { Readable } from 'node:stream';
import { requestOnce, parseReview, summarize } from './probe.mjs';

// Authored fixtures only. Every transport test supplies its own in-memory get.
function exchange(status, body, inspect = () => {}) {
  let calls = 0;
  const get = (url, opts, receive) => {
    calls++;
    inspect(url, opts);
    const req = new EventEmitter();
    req.destroy = () => {};
    queueMicrotask(() => {
      const res = Readable.from([Buffer.from(body)]);
      res.statusCode = status;
      receive(res);
    });
    return req;
  };
  return { get, count: () => calls };
}
const trial = transport => requestOnce('example-id', 'example-key', transport);
const vacancy = { id: 'v1', title: 'Java developer', redirect_url: 'https://www.adzuna.es/details/123', company: { display_name: 'Example' }, location: { area: ['Madrid'] }, created: '2026-11-13', description: 'Fictional Java vacancy.' };

test('uses the fixed Spain query once and returns rows in order', async () => {
  const net = exchange(200, JSON.stringify({ results: [vacancy] }), (u, opts) => {
    assert.equal(u.origin + u.pathname, 'https://api.adzuna.com/v1/api/jobs/es/search/1');
    assert.deepEqual(Object.fromEntries(u.searchParams), { what: 'Java', where: 'Madrid', results_per_page: '10', app_id: 'example-id', app_key: 'example-key' });
    assert.equal(opts.headers.Accept, 'application/json');
  });
  assert.deepEqual(await trial(net), [vacancy]);
  assert.equal(net.count(), 1);
});
test('redirects, authorization failures and quota responses stop after one request', async () => {
  for (const status of [302, 401, 403, 429]) {
    const net = exchange(status, 'example-key');
    await assert.rejects(trial(net), e => e.status === status && e.message === 'http_error');
    assert.equal(net.count(), 1);
  }
});
test('rejects malformed, unexpectedly large or over-limit responses', async () => {
  for (const [body, code] of [['{','invalid_json'], ['{}','invalid_results'], ['{"results":[null]}','invalid_results'],
    [JSON.stringify({ results: Array(11).fill({}) }),'invalid_results'], ['x'.repeat(512 * 1024 + 1),'response_too_large']]) {
    const net = exchange(200, body);
    await assert.rejects(trial(net), e => e.status === 200 && e.code === code);
    assert.equal(net.count(), 1);
  }
});
test('missing credentials do not reach the transport; transport errors reveal no URL', async () => {
  let calls = 0;
  const get = u => { calls++; throw new Error(u.href); };
  assert.throws(() => requestOnce('', 'x', { get }), e => e.code === 'missing_credentials');
  assert.equal(calls, 0);
  await assert.rejects(trial({ get }), e => e.message === 'network_error' && e.status === null);
  assert.equal(calls, 1);
});
test('a stalled request is destroyed without retry', async () => {
  let destroyed = 0, calls = 0;
  const get = () => { calls++; const req = new EventEmitter(); req.destroy = () => destroyed++; return req; };
  await assert.rejects(trial({ get, timeoutMs: 5 }), e => e.code === 'timeout');
  assert.equal(calls, 1);
  assert.equal(destroyed, 1);
});
test('an empty successful search records zero observations', async () => {
  const net = exchange(200, '{"results":[]}');
  const report = summarize(await trial(net), []);
  assert.equal(report.httpStatus, 200);
  assert.equal(report.returned, 0);
  assert.equal(report.decision, 'insufficient_evidence');
  assert.equal(net.count(), 1);
});
test('review accepts explicit evidence and leaves absent facts unknown', () => {
  assert.deepEqual(parseReview('Y H ?'), { relevance: 'yes', mode: 'hybrid', days: null });
  assert.deepEqual(parseReview(''), { relevance: 'uncertain', mode: 'unknown', days: null });
  assert.equal(parseReview('y r 0').days, 0);
  assert.equal(parseReview('y h'), null);
  assert.equal(parseReview('y h 6'), null);
});
test('decision requires one distinct relevant row with ID, title and safe referral together', () => {
  const rows = [vacancy, { ...vacancy }, { title: 'other' }, { ...vacancy, id: 'v2', title: '' }];
  const report = summarize(rows, ['y h ?', 'y u ?', 'n o 3', 'y u ?'].map(parseReview));
  assert.equal(report.returned, 4);
  assert.equal(report.distinctIds, 2);
  assert.equal(report.duplicateRows, 1);
  assert.equal(report.usableRelevantDistinct, 1);
  assert.equal(report.explicitWeeklyDays, 1);
  assert.deepEqual(report.missing, { id: 1, title: 1, company: 1, location: 1, created: 1, excerpt: 1, usableReferral: 1 });
  assert.deepEqual(report.relevantSoftwareInMadrid, { yes: 3, no: 1, uncertain: 0 });
  assert.deepEqual(report.workMode, { remote: 0, hybrid: 1, onsite: 1, unknown: 2 });
  assert.equal(report.decision, 'candidate_for_connector_design');
  assert.equal(JSON.stringify(report).includes(vacancy.title), false);
  assert.equal(JSON.stringify(report).includes(vacancy.redirect_url), false);
  assert.equal(summarize(rows, []).decision, 'insufficient_evidence');
});
test('rejects untrusted referral shapes without opening any link', () => {
  const links = ['http://www.adzuna.es/details/1', 'https://adzuna.es.invalid/details/1', 'https://x@adzuna.es/details/1',
    'https://adzuna.es:8080/details/1', 'https://adzuna.es/', 'https://adzuna.es/details/1?app_key=example', null];
  const report = summarize(links.map((redirect_url, id) => ({ ...vacancy, id, redirect_url })), links.map(() => parseReview('y u ?')));
  assert.equal(report.usableRelevantDistinct, 0);
  assert.equal(report.missing.usableReferral, links.length);
});
test('keeps absent, invalid, advertised, estimated and unknown-origin salary distinct', () => {
  const rows = [{ salary_min: 45000, salary_is_predicted: '0' }, { salary_max: 50000, salary_is_predicted: 1 },
    { salary_min: 40000 }, {}, { salary_min: 50000, salary_max: 30000 }, { salary_max: 0 }];
  assert.deepEqual(summarize(rows, []).salary, { advertised: 1, predicted: 1, unspecifiedOrigin: 1, missing: 1, invalidBounds: 2 });
});
