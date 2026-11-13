// Opt-in local evaluation only. Importing this module never calls the provider.
import { get as httpsGet } from 'node:https';
import { Writable } from 'node:stream';
import { createInterface } from 'node:readline/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const MAX_BYTES = 512 * 1024;
const modes = { r: 'remote', h: 'hybrid', o: 'onsite', u: 'unknown' };
const allowedHosts = new Set(['adzuna.es', 'adzuna.com.es', 'adzuna.co.uk', 'adzuna.com']
  .flatMap(h => [h, 'www.' + h]));
const nonempty = s => typeof s === 'string' && !!s.trim();
const idOf = v => nonempty(v) ? v.trim() : Number.isSafeInteger(v) && v >= 0 ? String(v) : null;

class ProbeError extends Error {
  constructor(code, status = null) { super(code); this.code = code; this.status = status; }
}

function safeReferral(value) {
  if (!nonempty(value)) return false;
  try {
    const url = new URL(value);
    return url.protocol === 'https:' && allowedHosts.has(url.hostname) && !url.username && !url.password
      && !url.port && url.pathname !== '/' && ![...url.searchParams.keys()].some(k => /^app_(id|key)$/i.test(k));
  } catch { return false; }
}

function validateRows(payload) {
  if (!Array.isArray(payload?.results) || payload.results.length > 10
    || !payload.results.every(r => r && typeof r === 'object' && !Array.isArray(r))) {
    throw new ProbeError('invalid_results', 200);
  }
  return payload.results;
}

/** One request, no redirect handling, no pagination and no retry. Never propagate transport messages. */
export function requestOnce(id, key, { get = httpsGet, timeoutMs = 15000 } = {}) {
  if (!nonempty(id) || !nonempty(key)) throw new ProbeError('missing_credentials');
  const url = new URL('https://api.adzuna.com/v1/api/jobs/es/search/1');
  url.search = new URLSearchParams({ what: 'Java', where: 'Madrid', results_per_page: '10', app_id: id, app_key: key });
  return new Promise((accept, reject) => {
    let req, status = null;
    const fail = code => { clearTimeout(timer); reject(new ProbeError(code, status)); };
    const timer = setTimeout(() => { fail('timeout'); req?.destroy(); }, timeoutMs);
    try {
      req = get(url, { agent: false, headers: { Accept: 'application/json' } }, res => {
        status = res.statusCode;
        res.on('error', () => fail('incomplete_response'));
        if (status !== 200) { fail('http_error'); res.destroy(); return; }
        const chunks = [];
        let size = 0;
        res.on('aborted', () => fail('incomplete_response'));
        res.on('data', chunk => {
          size += chunk.length;
          if (size > MAX_BYTES) { fail('response_too_large'); res.destroy(); return; }
          chunks.push(chunk);
        });
        res.on('end', () => {
          clearTimeout(timer);
          try { accept(validateRows(JSON.parse(Buffer.concat(chunks).toString('utf8')))); }
          catch (e) { fail(e instanceof ProbeError ? e.code : 'invalid_json'); }
        });
      });
      req.on('error', () => fail('network_error'));
    } catch { fail('network_error'); }
  });
}

// Counts describe returned rows; the viability criterion uses distinct usable, manually relevant IDs.
export function summarize(rows, reviews, date = new Date()) {
  const missing = { id: 0, title: 0, company: 0, location: 0, created: 0, excerpt: 0, usableReferral: 0 };
  const salary = { advertised: 0, predicted: 0, unspecifiedOrigin: 0, missing: 0, invalidBounds: 0 };
  const relevance = { yes: 0, no: 0, uncertain: 0 };
  const workMode = { remote: 0, hybrid: 0, onsite: 0, unknown: 0 };
  const ids = new Set(), viable = new Set();
  let duplicateRows = 0, explicitWeeklyDays = 0;
  for (const [i, row] of rows.entries()) {
    const id = idOf(row.id), title = nonempty(row.title), referral = safeReferral(row.redirect_url);
    if (id) { if (ids.has(id)) duplicateRows++; ids.add(id); } else missing.id++;
    if (!title) missing.title++;
    if (!referral) missing.usableReferral++;
    if (!nonempty(row.company?.display_name)) missing.company++;
    if (!nonempty(row.location?.display_name) && !(Array.isArray(row.location?.area) && row.location.area.some(nonempty))) missing.location++;
    if (!nonempty(row.created) || !Number.isFinite(Date.parse(row.created))) missing.created++;
    if (!nonempty(row.description)) missing.excerpt++;
    const bounds = [row.salary_min, row.salary_max].filter(b => b != null);
    if (!bounds.length) salary.missing++;
    else if (bounds.some(b => !Number.isFinite(b) || b <= 0) || (bounds.length === 2 && bounds[0] > bounds[1])) salary.invalidBounds++;
    else if ([1, '1', true].includes(row.salary_is_predicted)) salary.predicted++;
    else if ([0, '0', false].includes(row.salary_is_predicted)) salary.advertised++;
    else salary.unspecifiedOrigin++;

    const review = reviews[i] ?? { relevance: 'uncertain', mode: 'unknown', days: null };
    relevance[review.relevance]++;
    workMode[review.mode]++;
    if (review.days != null) explicitWeeklyDays++;
    if (review.relevance === 'yes' && id && title && referral) viable.add(id);
  }
  return { source: 'Adzuna', observedAt: date.toISOString(), market: 'es', query: 'Java / Madrid',
    httpStatus: 200, returned: rows.length, distinctIds: ids.size, duplicateRows,
    relevantSoftwareInMadrid: relevance, usableRelevantDistinct: viable.size, missing, salary, workMode, explicitWeeklyDays,
    decision: viable.size ? 'candidate_for_connector_design' : 'insufficient_evidence' };
}

export function parseReview(input) {
  if (!input.trim()) return { relevance: 'uncertain', mode: 'unknown', days: null };
  const m = /^([ynu])\s+([rhou])\s+([0-5?])$/i.exec(input.trim());
  if (!m) return null;
  return { relevance: ({ y: 'yes', n: 'no', u: 'uncertain' })[m[1].toLowerCase()],
    mode: modes[m[2].toLowerCase()], days: m[3] === '?' ? null : Number(m[3]) };
}

// Extract a short, inert preview. Avoid printing raw descriptions, URLs, terminal controls or credentials.
function preview(value, secrets) {
  let s = typeof value === 'string' ? value : '';
  for (const secret of secrets) for (const token of [secret, encodeURIComponent(secret)]) {
    if (token) s = s.replaceAll(token, '[redacted]');
  }
  return s.replace(/<[^>]*>/g, ' ').replace(/https?:\/\/\S+/gi, '[link]')
    .replace(/[\p{Cc}\p{Cf}]/gu, ' ').replace(/\s+/g, ' ').trim().slice(0, 700) || '(not supplied)';
}

async function main() {
  if (!process.stdin.isTTY || !process.stderr.isTTY || process.argv.length !== 2) {
    console.error('Run interactively without arguments. No request sent.'); process.exitCode = 2; return;
  }
  if (process.env.NODE_DEBUG || process.env.NODE_OPTIONS || process.env.NODE_DEBUG_NATIVE || process.env.SSLKEYLOGFILE) {
    console.error('Use a terminal without Node debug, tracing or TLS key logging options. No request sent.'); process.exitCode = 2; return;
  }
  let muted = false;
  const output = new Writable({ write(data, enc, done) { if (!muted) process.stderr.write(data); done(); } });
  const io = createInterface({ input: process.stdin, output, terminal: true, historySize: 0 });
  async function secret(label, fromEnv) {
    if (nonempty(fromEnv)) return fromEnv.trim();
    process.stderr.write(label + ' (hidden): '); muted = true;
    try { return (await io.question('')).trim(); }
    finally { muted = false; process.stderr.write('\n'); }
  }
  try {
    const confirmed = await io.question('Free evaluation plan and quota confirmed? Type YES for one live request: ');
    if (confirmed.trim() !== 'YES') { console.error('Cancelled. No request sent.'); return; }
    const id = await secret('Adzuna app_id', process.env.ADZUNA_APP_ID);
    const key = await secret('Adzuna app_key', process.env.ADZUNA_APP_KEY);
    const rows = await requestOnce(id, key);
    const observedAt = new Date(), reviews = [];
    if (rows.length) console.error('Review in this terminal only. Is this a Java software vacancy in Madrid? y/n/u. Explicit mode: r(remote), h(hybrid), o(on-site), u(unknown). Explicit office days/week: 0-5 or ?. Example: y h ?. Enter alone = all unknown.');
    for (const [i, row] of rows.entries()) {
      console.error('\n' + (i + 1) + '/' + rows.length + ' · Adzuna · ' + preview(row.title, [id, key]));
      console.error('Location: ' + preview(row.location?.display_name || (Array.isArray(row.location?.area) ? row.location.area.filter(nonempty).join(', ') : ''), [id, key]));
      console.error('Excerpt: ' + preview(row.description, [id, key]));
      let review;
      do { review = parseReview(await io.question('Relevance / mode / days: ')); } while (!review);
      reviews.push(review);
    }
    console.log(JSON.stringify(summarize(rows, reviews, observedAt), null, 2));
  } catch (e) {
    console.log(JSON.stringify({ source: 'Adzuna', observedAt: new Date().toISOString(), market: 'es', query: 'Java / Madrid',
      httpStatus: e instanceof ProbeError ? e.status : null, outcome: e instanceof ProbeError ? e.code : 'probe_interrupted' }));
    process.exitCode = 1;
  } finally { io.close(); }
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) await main();
