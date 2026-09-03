/**
 * One-shot sweeper for leftover em_workflow rows (old and new naming).
 *
 * Matches:
 *   engagements — summary "k6 em_workflow", or name "LT Engagement …" / "k6-…"
 *   clients     — company name "LT Co …" / "k6-…"
 *
 * Assignments cascade off engagement delete. Auth users are not deleted.
 *
 *   k6 run cleanup-loadtest-data.js
 *   k6 run --env DRY_RUN=1 cleanup-loadtest-data.js
 */
import http from 'k6/http';
import { sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL
  || 'http://api-gateway-test-alb-568701708.us-east-1.elb.amazonaws.com';

const EM_EMAIL = __ENV.EM_EMAIL || 'test.manager3@skillstorm-test.com';
const EM_PASSWORD = __ENV.EM_PASSWORD || 'TestPassword123!';

function envEnabled(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined || raw === '') return defaultValue;
  return !['0', 'false', 'no', 'off'].includes(String(raw).toLowerCase());
}

const DRY_RUN = envEnabled('DRY_RUN', false);

export const options = {
  vus: 1,
  iterations: 1,
};

function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h.Authorization = `Bearer ${token}`;
  return { headers: h };
}

function parseList(res) {
  try {
    const body = res.json();
    if (Array.isArray(body)) return body;
    if (body && Array.isArray(body.content)) return body.content;
  } catch (_) {}
  return [];
}

function isLoadtestEngagement(e) {
  const name = e && e.engagementName ? String(e.engagementName) : '';
  const summary = e && e.summary ? String(e.summary) : '';
  return summary === 'k6 em_workflow'
    || name.indexOf('LT Engagement ') === 0
    || name.indexOf('k6-') === 0;
}

function isLoadtestClient(c) {
  const name = c && c.companyName ? String(c.companyName) : '';
  return name.indexOf('LT Co ') === 0 || name.indexOf('k6-') === 0;
}

function login() {
  const res = http.post(
    `${BASE_URL}/auth/api/auth/login`,
    JSON.stringify({ email: EM_EMAIL, password: EM_PASSWORD }),
    jsonHeaders()
  );
  if (res.status !== 200) {
    console.error(`login failed: ${res.status} ${res.body}`);
    return null;
  }
  try {
    return res.json().accessToken;
  } catch (_) {
    return null;
  }
}

function uniqueById(items) {
  const seen = {};
  const out = [];
  for (let i = 0; i < items.length; i++) {
    const id = items[i] && items[i].id;
    if (id == null || seen[id]) continue;
    seen[id] = true;
    out.push(items[i]);
  }
  return out;
}

function batchDelete(urls, opts) {
  const chunkSize = 20;
  let ok = 0;
  let fail = 0;
  for (let i = 0; i < urls.length; i += chunkSize) {
    const chunk = urls.slice(i, i + chunkSize).map((url) => ['DELETE', url, null, opts]);
    const responses = http.batch(chunk);
    for (let r = 0; r < responses.length; r++) {
      const res = responses[r];
      if (res.status === 204 || res.status === 200 || res.status === 404) {
        ok += 1;
      } else {
        fail += 1;
        console.warn(`DELETE ${chunk[r][1]} -> ${res.status} ${res.body}`);
      }
    }
    sleep(0.2);
  }
  return { ok, fail };
}

function searchEngagements(q, opts) {
  const res = http.get(
    `${BASE_URL}/engagement/api/engagements/search?q=${encodeURIComponent(q)}`,
    opts
  );
  if (res.status !== 200) {
    console.warn(`engagement search "${q}" -> ${res.status}`);
    return [];
  }
  return parseList(res);
}

function listAllEngagements(opts) {
  const res = http.get(`${BASE_URL}/engagement/api/engagements`, opts);
  if (res.status !== 200) {
    console.warn(`engagement list -> ${res.status}`);
    return [];
  }
  return parseList(res);
}

function searchClients(q, opts) {
  const res = http.get(
    `${BASE_URL}/client/clients/search?q=${encodeURIComponent(q)}`,
    opts
  );
  if (res.status !== 200) {
    console.warn(`client search "${q}" -> ${res.status}`);
    return [];
  }
  return parseList(res);
}

function listAllClients(opts) {
  const all = [];
  for (let page = 0; page < 50; page++) {
    const res = http.get(`${BASE_URL}/client/clients?page=${page}&size=100`, opts);
    if (res.status !== 200) break;
    const batch = parseList(res);
    if (batch.length === 0) break;
    for (let i = 0; i < batch.length; i++) all.push(batch[i]);
    if (batch.length < 100) break;
  }
  return all;
}

export default function () {
  const token = login();
  if (!token) {
    return;
  }
  const opts = jsonHeaders(token);

  const engagements = uniqueById(
    listAllEngagements(opts)
      .concat(searchEngagements('k6 em_workflow', opts))
      .concat(searchEngagements('LT Engagement', opts))
      .filter(isLoadtestEngagement)
  );

  const clients = uniqueById(
    listAllClients(opts)
      .concat(searchClients('LT Co', opts))
      .concat(searchClients('k6-', opts))
      .filter(isLoadtestClient)
  );

  console.log(
    `found ${engagements.length} load-test engagements, ${clients.length} load-test clients`
  );
  for (let i = 0; i < Math.min(5, engagements.length); i++) {
    console.log(
      `  eng ${engagements[i].id} "${engagements[i].engagementName}" summary="${engagements[i].summary}"`
    );
  }
  for (let i = 0; i < Math.min(5, clients.length); i++) {
    console.log(`  client ${clients[i].id} "${clients[i].companyName}"`);
  }

  if (DRY_RUN) {
    console.log('DRY_RUN=1 — not deleting');
    return;
  }

  const engUrls = engagements.map((e) => `${BASE_URL}/engagement/api/engagements/${e.id}`);
  const engResult = batchDelete(engUrls, opts);

  const clientUrls = clients.map((c) => `${BASE_URL}/client/clients/${c.id}`);
  const clientResult = batchDelete(clientUrls, opts);

  console.log(
    `deleted engagements ${engResult.ok}/${engagements.length}` +
      ` clients ${clientResult.ok}/${clients.length}` +
      (engResult.fail + clientResult.fail
        ? ` failures ${engResult.fail + clientResult.fail}`
        : '')
  );
}
