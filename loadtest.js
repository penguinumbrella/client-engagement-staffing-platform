/**
 * Platform load / capacity suite.
 *
 * Scenarios (run together by default):
 *   1. logged_in_browse  — EM sessions browsing engagements/staffing (capacity for concurrent users)
 *   2. em_workflow       — EM write path: client → engagement (sync client check) → assign → notifications
 *   3. consultant_rbac   — consultant session + RBAC (allowed /me, forbidden EM writes)
 *   4. login_stampede    — concurrent login burst (characterizes auth CPU limit)
 *
 * Writes are tagged with a per-run id and removed in teardown (engagements
 * first so client delete is not 409, then clients, then a consultant this
 * run registered). Deletes are the services' normal soft-deletes. Auth users
 * have no delete API, so a registered consultant login may remain.
 *
 * Env overrides (optional):
 *   BASE_URL, EM_EMAIL, EM_PASSWORD, CONSULTANT_EMAIL, CONSULTANT_PASSWORD
 *   SKIP_WRITES=1  — skip em_workflow creates (and skip registering a consultant)
 *   CLEANUP=0      — leave this run's rows in the DB (default is to clean up)
 *
 * Run:  k6 run loadtest.js
 * One scenario: k6 run --env SCENARIO=em_workflow loadtest.js  (see options filter below)
 */
import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL
  || 'http://api-gateway-test-alb-568701708.us-east-1.elb.amazonaws.com';

const EM_EMAIL = __ENV.EM_EMAIL || 'test.manager3@skillstorm-test.com';
const EM_PASSWORD = __ENV.EM_PASSWORD || 'TestPassword123!';

// --- metrics ---
const loginErrors = new Rate('login_errors');
const loginDuration = new Trend('login_duration');
const loginRetries = new Rate('login_retries');

const browseEngagementErrors = new Rate('browse_engagement_errors');
const browseEngagementDuration = new Trend('browse_engagement_duration');
const browseStaffingErrors = new Rate('browse_staffing_errors');
const browseStaffingDuration = new Trend('browse_staffing_duration');

const clientWriteErrors = new Rate('client_write_errors');
const clientWriteDuration = new Trend('client_write_duration');
const engagementWriteErrors = new Rate('engagement_write_errors');
const engagementWriteDuration = new Trend('engagement_write_duration');
const assignmentWriteErrors = new Rate('assignment_write_errors');
const assignmentWriteDuration = new Trend('assignment_write_duration');
const notificationErrors = new Rate('notification_errors');
const notificationDuration = new Trend('notification_duration');

const rbacAllowedErrors = new Rate('rbac_allowed_errors');
const rbacDeniedOk = new Rate('rbac_denied_correct'); // 1 = got 403 as expected
const stampedeLoginErrors = new Rate('stampede_login_errors');
const stampedeLoginDuration = new Trend('stampede_login_duration');

const httpStatus = new Counter('http_status_total');

// Per-VU session state
const emTokens = {};
const consultantTokens = {};
const loginRecorded = {};

const ONLY = __ENV.SCENARIO; // optional: logged_in_browse | em_workflow | consultant_rbac | login_stampede

function envEnabled(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined || raw === '') return defaultValue;
  return !['0', 'false', 'no', 'off'].includes(String(raw).toLowerCase());
}

const SKIP_WRITES = envEnabled('SKIP_WRITES', false);
const DO_CLEANUP = envEnabled('CLEANUP', true);

function scenarioEnabled(name) {
  return !ONLY || ONLY === name;
}

function buildScenarios() {
  const fullSuite = !ONLY;
  const scenarios = {};
  if (scenarioEnabled('logged_in_browse')) {
    scenarios.logged_in_browse = {
      executor: 'ramping-vus',
      exec: 'browseAsEm',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 5 },
        { duration: '30s', target: 20 },
        { duration: '30s', target: 40 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '20s',
    };
  }
  if (scenarioEnabled('em_workflow') && (!SKIP_WRITES || ONLY === 'em_workflow')) {
    scenarios.em_workflow = {
      executor: 'ramping-vus',
      exec: 'emWorkflow',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 3 },
        { duration: '40s', target: 8 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '20s',
      startTime: fullSuite ? '5s' : '0s',
    };
  }
  if (scenarioEnabled('consultant_rbac') && (!SKIP_WRITES || __ENV.CONSULTANT_EMAIL)) {
    scenarios.consultant_rbac = {
      executor: 'ramping-vus',
      exec: 'consultantRbac',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 3 },
        { duration: '30s', target: 8 },
        { duration: '15s', target: 0 },
      ],
      startTime: fullSuite ? '10s' : '0s',
    };
  }
  if (scenarioEnabled('login_stampede')) {
    scenarios.login_stampede = {
      executor: 'ramping-vus',
      exec: 'loginStampede',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 10 },
        { duration: '20s', target: 25 },
        { duration: '15s', target: 0 },
      ],
      startTime: fullSuite ? '2m' : '0s',
    };
  }
  return scenarios;
}

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    login_errors: ['rate<0.1'],
    browse_engagement_errors: ['rate<0.05'],
    browse_staffing_errors: ['rate<0.05'],
    client_write_errors: ['rate<0.15'],
    engagement_write_errors: ['rate<0.15'],
    assignment_write_errors: ['rate<0.2'],
    notification_errors: ['rate<0.2'],
    rbac_allowed_errors: ['rate<0.1'],
    // stampede is expected to be harsher — allow higher failure rate
    stampede_login_errors: ['rate<0.5'],
  },
};

// ---------- helpers ----------

function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h.Authorization = `Bearer ${token}`;
  return { headers: h };
}

function trackStatus(res) {
  httpStatus.add(1, { status: String(res.status) });
}

function tryLogin(email, password) {
  const res = http.post(
    `${BASE_URL}/auth/api/auth/login`,
    JSON.stringify({ email, password }),
    jsonHeaders()
  );
  trackStatus(res);
  loginDuration.add(res.timings.duration);
  if (res.status !== 200) return null;
  try {
    const body = res.json();
    return {
      token: body.accessToken,
      userId: body.user && body.user.id,
      role: body.user && body.user.role,
    };
  } catch (_) {
    return null;
  }
}

function ensureEmSession() {
  if (emTokens[__VU]) return emTokens[__VU];

  sleep((__VU % 10) * 0.2);

  let usedRetry = false;
  for (let attempt = 1; attempt <= 5; attempt++) {
    const session = tryLogin(EM_EMAIL, EM_PASSWORD);
    if (session && session.token) {
      emTokens[__VU] = session;
      if (!loginRecorded[`em-${__VU}`]) {
        loginErrors.add(0);
        loginRetries.add(usedRetry ? 1 : 0);
        loginRecorded[`em-${__VU}`] = true;
        check(true, { 'EM login session established': () => true });
      }
      return session;
    }
    usedRetry = true;
    sleep(0.4 * attempt);
  }

  if (!loginRecorded[`em-${__VU}`]) {
    loginErrors.add(1);
    loginRetries.add(1);
    loginRecorded[`em-${__VU}`] = true;
    check(false, { 'EM login session established': () => false });
  }
  return null;
}

function parseList(res) {
  try {
    const body = res.json();
    if (Array.isArray(body)) return body;
    if (body && Array.isArray(body.content)) return body.content;
  } catch (_) {}
  return [];
}

// ---------- setup: seed a consultant account for RBAC ----------

export function setup() {
  const runId = `${Date.now()}`;
  const consultantEmail =
    __ENV.CONSULTANT_EMAIL ||
    `loadtest.consultant.${runId}@skillstorm-test.com`;
  const consultantPassword = __ENV.CONSULTANT_PASSWORD || 'TestPassword123!';

  let consultantToken = null;
  let consultantUserId = null;
  let createdConsultant = false;
  let seededConsultantId = null;

  if (__ENV.CONSULTANT_EMAIL) {
    const session = tryLogin(consultantEmail, consultantPassword);
    if (session) {
      consultantToken = session.token;
      consultantUserId = session.userId;
    }
  }

  if (!consultantToken && !SKIP_WRITES) {
    const reg = http.post(
      `${BASE_URL}/auth/api/auth/register`,
      JSON.stringify({
        firstName: 'Load',
        lastName: 'Consultant',
        email: consultantEmail,
        password: consultantPassword,
        titleRole: 'Associate',
        primarySkillArea: 'Audit',
      }),
      jsonHeaders()
    );
    trackStatus(reg);
    if (reg.status === 201 || reg.status === 200) {
      createdConsultant = true;
      try {
        const body = reg.json();
        consultantToken = body.accessToken;
        consultantUserId = body.user && body.user.id;
      } catch (_) {}
    } else {
      // maybe already exists
      const session = tryLogin(consultantEmail, consultantPassword);
      if (session) {
        consultantToken = session.token;
        consultantUserId = session.userId;
      }
    }
  }

  if (createdConsultant && consultantToken) {
    const meRes = http.get(
      `${BASE_URL}/staffing/api/consultants/me`,
      jsonHeaders(consultantToken)
    );
    trackStatus(meRes);
    if (meRes.status === 200) {
      try {
        seededConsultantId = meRes.json('id');
      } catch (_) {}
    }
  }

  const em = tryLogin(EM_EMAIL, EM_PASSWORD);
  if (!em) {
    fail('setup: EM login failed — cannot seed load test');
  }

  // Discover an existing consultant id for assignments
  const consultantsRes = http.get(
    `${BASE_URL}/staffing/api/consultants`,
    jsonHeaders(em.token)
  );
  const consultants = parseList(consultantsRes);
  const consultantIds = consultants.map((c) => c.id).filter(Boolean);

  return {
    runId,
    consultantEmail,
    consultantPassword,
    consultantToken,
    consultantUserId,
    consultantIds,
    createdConsultant,
    seededConsultantId,
    emUserId: em.userId,
  };
}

// ---------- 1) Logged-in EM browse ----------

export function browseAsEm() {
  const session = ensureEmSession();
  if (!session) {
    sleep(1);
    return;
  }
  const opts = jsonHeaders(session.token);

  const engagementRes = http.get(`${BASE_URL}/engagement/api/engagements`, opts);
  trackStatus(engagementRes);
  browseEngagementDuration.add(engagementRes.timings.duration);
  browseEngagementErrors.add(engagementRes.status !== 200);
  check(engagementRes, { 'browse: engagement list 200': (r) => r.status === 200 });

  const list = parseList(engagementRes);
  if (list.length > 0 && list[0].id) {
    const id = list[0].id;
    const detailRes = http.get(`${BASE_URL}/engagement/api/engagements/${id}`, opts);
    trackStatus(detailRes);
    browseEngagementDuration.add(detailRes.timings.duration);
    browseEngagementErrors.add(detailRes.status !== 200);
    check(detailRes, { 'browse: engagement detail 200': (r) => r.status === 200 });

    const assignRes = http.get(
      `${BASE_URL}/staffing/api/assignments/engagement/${id}`,
      opts
    );
    trackStatus(assignRes);
    browseStaffingDuration.add(assignRes.timings.duration);
    browseStaffingErrors.add(assignRes.status !== 200);
    check(assignRes, { 'browse: assignments by engagement 200': (r) => r.status === 200 });
  }

  const clientsRes = http.get(`${BASE_URL}/client/clients?page=0&size=20`, opts);
  trackStatus(clientsRes);
  browseEngagementDuration.add(clientsRes.timings.duration);
  // clients returns Page — 200 expected for EM
  browseEngagementErrors.add(clientsRes.status !== 200);
  check(clientsRes, { 'browse: clients list 200': (r) => r.status === 200 });

  const consultantsRes = http.get(`${BASE_URL}/staffing/api/consultants`, opts);
  trackStatus(consultantsRes);
  browseStaffingDuration.add(consultantsRes.timings.duration);
  browseStaffingErrors.add(consultantsRes.status !== 200);
  check(consultantsRes, { 'browse: consultants list 200': (r) => r.status === 200 });

  sleep(1);
}

// ---------- 2) EM write workflow (client → engagement → assign → notify) ----------

export function emWorkflow(data) {
  if (SKIP_WRITES && ONLY !== 'em_workflow') {
    sleep(1);
    return;
  }
  const session = ensureEmSession();
  if (!session) {
    sleep(1);
    return;
  }
  const opts = jsonHeaders(session.token);
  const runId = (data && data.runId) || 'unknown';
  const suffix = `${__VU}-${__ITER}`;

  // Create client (unique company name tagged with this run for teardown)
  const clientRes = http.post(
    `${BASE_URL}/client/clients`,
    JSON.stringify({
      companyName: `k6-${runId} Co ${suffix}`,
      industry: 'Technology',
      primaryContactName: 'Load Test',
      primaryContactEmail: `lt-${runId}-${suffix}@example.com`,
      relationshipStatus: 'ACTIVE',
    }),
    opts
  );
  trackStatus(clientRes);
  clientWriteDuration.add(clientRes.timings.duration);
  const clientOk = clientRes.status === 201 || clientRes.status === 200;
  clientWriteErrors.add(!clientOk);
  check(clientRes, { 'write: create client 2xx': (r) => r.status === 201 || r.status === 200 });

  let clientId = null;
  if (clientOk) {
    try {
      clientId = clientRes.json('id');
    } catch (_) {}
  }
  if (!clientId) {
    sleep(1);
    return;
  }

  // Create engagement (sync client validation path)
  const engRes = http.post(
    `${BASE_URL}/engagement/api/engagements`,
    JSON.stringify({
      engagementName: `k6-${runId} Eng ${suffix}`,
      clientId,
      engagementType: 'Audit',
      summary: 'k6 em_workflow',
      startDate: '2026-10-01',
      targetEndDate: '2026-12-31',
      status: 'Planned',
    }),
    opts
  );
  trackStatus(engRes);
  engagementWriteDuration.add(engRes.timings.duration);
  const engOk = engRes.status === 201 || engRes.status === 200;
  engagementWriteErrors.add(!engOk);
  check(engRes, { 'write: create engagement 2xx': (r) => r.status === 201 || r.status === 200 });

  let engagementId = null;
  if (engOk) {
    try {
      engagementId = engRes.json('id');
    } catch (_) {}
  }
  if (!engagementId) {
    sleep(1);
    return;
  }

  // Assign an existing consultant (rotate by VU)
  const ids = (data && data.consultantIds) || [];
  if (ids.length === 0) {
    sleep(1);
    return;
  }
  const consultantId = ids[__VU % ids.length];

  const asgRes = http.post(
    `${BASE_URL}/staffing/api/assignments`,
    JSON.stringify({
      consultantId,
      engagementId,
      engagementRole: 'Associate',
      assignmentStartDate: '2026-10-01',
      assignmentEndDate: '2026-12-31',
    }),
    opts
  );
  trackStatus(asgRes);
  assignmentWriteDuration.add(asgRes.timings.duration);
  // 201 created, or 200/409 if duplicate handling
  const asgOk = asgRes.status === 201 || asgRes.status === 200;
  assignmentWriteErrors.add(!asgOk);
  check(asgRes, {
    'write: assign consultant 2xx': (r) => r.status === 201 || r.status === 200,
  });

  // Read back staffing on engagement
  const listAsg = http.get(
    `${BASE_URL}/staffing/api/assignments/engagement/${engagementId}`,
    opts
  );
  trackStatus(listAsg);
  browseStaffingDuration.add(listAsg.timings.duration);
  browseStaffingErrors.add(listAsg.status !== 200);
  check(listAsg, { 'write: list assignments 200': (r) => r.status === 200 });

  // Notification feed for EM (async path may lag — accept 200 with any body)
  const notifRes = http.get(
    `${BASE_URL}/notification/api/notifications?recipientId=${session.userId}`,
    opts
  );
  trackStatus(notifRes);
  notificationDuration.add(notifRes.timings.duration);
  notificationErrors.add(notifRes.status !== 200);
  check(notifRes, { 'write: notifications 200': (r) => r.status === 200 });

  sleep(1);
}

// ---------- 3) Consultant RBAC ----------

export function consultantRbac(data) {
  if (!data || !data.consultantToken) {
    // try login with seeded credentials
    if (data && data.consultantEmail) {
      const session = tryLogin(data.consultantEmail, data.consultantPassword);
      if (session) {
        consultantTokens[__VU] = session;
      }
    }
  } else if (!consultantTokens[__VU]) {
    consultantTokens[__VU] = {
      token: data.consultantToken,
      userId: data.consultantUserId,
    };
  }

  const session = consultantTokens[__VU];
  if (!session || !session.token) {
    sleep(1);
    return;
  }
  const opts = jsonHeaders(session.token);

  // Allowed: own assignments
  const mine = http.get(`${BASE_URL}/staffing/api/assignments/me`, opts);
  trackStatus(mine);
  const mineOk = mine.status === 200;
  rbacAllowedErrors.add(!mineOk);
  check(mine, { 'rbac: consultant /assignments/me 200': (r) => r.status === 200 });

  // Allowed: auth me
  const me = http.get(`${BASE_URL}/auth/api/auth/me`, opts);
  trackStatus(me);
  rbacAllowedErrors.add(me.status !== 200);
  check(me, { 'rbac: consultant /auth/me 200': (r) => r.status === 200 });

  // Forbidden: create client (EM only) — expect 403
  const forbidden = http.post(
    `${BASE_URL}/client/clients`,
    JSON.stringify({
      companyName: `Should Fail ${__VU}-${Date.now()}`,
      industry: 'Technology',
      primaryContactName: 'Nope',
      primaryContactEmail: 'nope@example.com',
      relationshipStatus: 'ACTIVE',
    }),
    opts
  );
  trackStatus(forbidden);
  const deniedCorrect = forbidden.status === 403;
  rbacDeniedOk.add(deniedCorrect ? 1 : 0);
  check(forbidden, { 'rbac: consultant create client is 403': (r) => r.status === 403 });

  // Forbidden: create engagement
  const forbidEng = http.post(
    `${BASE_URL}/engagement/api/engagements`,
    JSON.stringify({
      engagementName: 'Should Fail',
      clientId: 1,
      engagementType: 'Audit',
      startDate: '2026-10-01',
      targetEndDate: '2026-12-31',
      status: 'Planned',
    }),
    opts
  );
  trackStatus(forbidEng);
  rbacDeniedOk.add(forbidEng.status === 403 ? 1 : 0);
  check(forbidEng, { 'rbac: consultant create engagement is 403': (r) => r.status === 403 });

  sleep(1);
}

// ---------- 4) Login stampede ----------

export function loginStampede() {
  const res = http.post(
    `${BASE_URL}/auth/api/auth/login`,
    JSON.stringify({ email: EM_EMAIL, password: EM_PASSWORD }),
    jsonHeaders()
  );
  trackStatus(res);
  stampedeLoginDuration.add(res.timings.duration);
  stampedeLoginErrors.add(res.status !== 200);
  check(res, { 'stampede: login 200': (r) => r.status === 200 });
  sleep(0.5);
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
      trackStatus(res);
      if (res.status === 204 || res.status === 200 || res.status === 404) {
        ok += 1;
      } else {
        fail += 1;
        console.warn(`cleanup DELETE ${chunk[r][1]} -> ${res.status}`);
      }
    }
  }
  return { ok, fail };
}

/*
 * k6 VUs cannot pass created ids back to teardown, so em_workflow tags
 * company/engagement names with setup's runId and we search those back out.
 * Engagements must go first: client delete returns 409 while any are active.
 * Assignment rows are cancelled by engagement delete's staffing cascade.
 */
export function teardown(data) {
  if (!DO_CLEANUP) {
    console.log('CLEANUP=0 — leaving this run\'s rows in the DB');
    return;
  }
  if (!data || !data.runId) {
    console.warn('teardown: no runId from setup; skipping cleanup');
    return;
  }

  const em = tryLogin(EM_EMAIL, EM_PASSWORD);
  if (!em || !em.token) {
    console.warn('teardown: EM login failed; cannot clean up this run\'s data');
    return;
  }
  const opts = jsonHeaders(em.token);
  const tag = `k6-${data.runId}`;

  const searchRes = http.get(
    `${BASE_URL}/client/clients/search?q=${encodeURIComponent(tag)}`,
    opts
  );
  trackStatus(searchRes);
  const clients = parseList(searchRes).filter((c) =>
    c && c.id && String(c.companyName || '').indexOf(tag) !== -1
  );

  const engagementUrls = [];
  for (let i = 0; i < clients.length; i++) {
    const listRes = http.get(
      `${BASE_URL}/engagement/api/engagements/client/${clients[i].id}`,
      opts
    );
    trackStatus(listRes);
    const engagements = parseList(listRes);
    for (let e = 0; e < engagements.length; e++) {
      if (engagements[e] && engagements[e].id) {
        engagementUrls.push(`${BASE_URL}/engagement/api/engagements/${engagements[e].id}`);
      }
    }
  }

  const engResult = batchDelete(engagementUrls, opts);
  const clientUrls = clients.map((c) => `${BASE_URL}/client/clients/${c.id}`);
  const clientResult = batchDelete(clientUrls, opts);

  let consultantDeleted = 0;
  if (data.createdConsultant && data.seededConsultantId) {
    const del = http.del(
      `${BASE_URL}/staffing/api/consultants/${data.seededConsultantId}`,
      null,
      opts
    );
    trackStatus(del);
    if (del.status === 204 || del.status === 200 || del.status === 404) {
      consultantDeleted = 1;
    } else {
      console.warn(`cleanup DELETE consultant ${data.seededConsultantId} -> ${del.status}`);
    }
  }

  console.log(
    `cleanup run ${data.runId}: engagements ${engResult.ok}/${engagementUrls.length}` +
      ` clients ${clientResult.ok}/${clients.length}` +
      ` consultant ${consultantDeleted}` +
      (engResult.fail + clientResult.fail ? ` failures ${engResult.fail + clientResult.fail}` : '')
  );

  sweepHistoricalLoadtestRows(opts);
}

function isHistoricalLoadtestEngagement(e) {
  const name = e && e.engagementName ? String(e.engagementName) : '';
  const summary = e && e.summary ? String(e.summary) : '';
  return summary === 'k6 em_workflow'
    || name.indexOf('LT Engagement ') === 0
    || name.indexOf('k6-') === 0;
}

function isHistoricalLoadtestClient(c) {
  const name = c && c.companyName ? String(c.companyName) : '';
  return name.indexOf('LT Co ') === 0 || name.indexOf('k6-') === 0;
}

function sweepHistoricalLoadtestRows(opts) {
  const engRes = http.get(`${BASE_URL}/engagement/api/engagements`, opts);
  const leftoverEng = parseList(engRes).filter(isHistoricalLoadtestEngagement);
  const leftoverEngUrls = leftoverEng.map((e) => `${BASE_URL}/engagement/api/engagements/${e.id}`);

  const clientRes = http.get(`${BASE_URL}/client/clients/search?q=${encodeURIComponent('LT Co')}`, opts);
  const k6ClientRes = http.get(`${BASE_URL}/client/clients/search?q=${encodeURIComponent('k6-')}`, opts);
  const leftoverClients = parseList(clientRes)
    .concat(parseList(k6ClientRes))
    .filter(isHistoricalLoadtestClient);
  const seen = {};
  const leftoverClientUrls = [];
  for (let i = 0; i < leftoverClients.length; i++) {
    const id = leftoverClients[i].id;
    if (id == null || seen[id]) continue;
    seen[id] = true;
    leftoverClientUrls.push(`${BASE_URL}/client/clients/${id}`);
  }

  if (leftoverEngUrls.length === 0 && leftoverClientUrls.length === 0) {
    return;
  }

  const engSweep = batchDelete(leftoverEngUrls, opts);
  const clientSweep = batchDelete(leftoverClientUrls, opts);
  console.log(
    `cleanup historical k6/LT tags: engagements ${engSweep.ok}/${leftoverEngUrls.length}` +
      ` clients ${clientSweep.ok}/${leftoverClientUrls.length}`
  );
}
