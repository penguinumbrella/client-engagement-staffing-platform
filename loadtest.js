import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = 'http://api-gateway-test-alb-568701708.us-east-1.elb.amazonaws.com';

const loginErrors = new Rate('login_errors');
const loginDuration = new Trend('login_duration');
const engagementErrors = new Rate('engagement_errors');
const engagementDuration = new Trend('engagement_duration');
const staffingErrors = new Rate('staffing_errors');
const staffingDuration = new Trend('staffing_duration');

// One token per VU — login once, then browse as a logged-in user
const tokens = {};

export const options = {
  scenarios: {
    logged_in_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '30s', target: 15 },
        { duration: '30s', target: 30 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    login_errors: ['rate<0.1'],
    engagement_errors: ['rate<0.1'],
    staffing_errors: ['rate<0.1'],
  },
};

function login() {
  const res = http.post(
    `${BASE_URL}/auth/api/auth/login`,
    JSON.stringify({
      email: 'test.manager3@skillstorm-test.com',
      password: 'TestPassword123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  loginDuration.add(res.timings.duration);
  loginErrors.add(res.status !== 200);
  check(res, { 'login status is 200': (r) => r.status === 200 });

  if (res.status !== 200) {
    return null;
  }
  try {
    return res.json('accessToken');
  } catch (_) {
    return null;
  }
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  // Each virtual user logs in once, then reuses the JWT (logged-in session load)
  if (!tokens[__VU]) {
    const token = login();
    if (!token) {
      sleep(1);
      return;
    }
    tokens[__VU] = token;
  }

  const opts = authHeaders(tokens[__VU]);

  // EM browsing: list engagements
  const engagementRes = http.get(`${BASE_URL}/engagement/api/engagements`, opts);
  engagementDuration.add(engagementRes.timings.duration);
  engagementErrors.add(engagementRes.status !== 200);
  check(engagementRes, { 'engagement list is 200': (r) => r.status === 200 });

  // Open first engagement detail when the list has data
  if (engagementRes.status === 200) {
    try {
      const body = engagementRes.json();
      const list = Array.isArray(body) ? body : body?.content;
      if (list && list.length > 0 && list[0].id) {
        const detailRes = http.get(
          `${BASE_URL}/engagement/api/engagements/${list[0].id}`,
          opts
        );
        engagementDuration.add(detailRes.timings.duration);
        engagementErrors.add(detailRes.status !== 200);
        check(detailRes, { 'engagement detail is 200': (r) => r.status === 200 });

        const assignRes = http.get(
          `${BASE_URL}/staffing/api/assignments/engagement/${list[0].id}`,
          opts
        );
        staffingDuration.add(assignRes.timings.duration);
        staffingErrors.add(assignRes.status !== 200);
        check(assignRes, { 'assignments by engagement is 200': (r) => r.status === 200 });
      }
    } catch (_) {
      // ignore parse errors; list check already recorded
    }
  }

  // EM browsing: consultants
  const consultantsRes = http.get(`${BASE_URL}/staffing/api/consultants`, opts);
  staffingDuration.add(consultantsRes.timings.duration);
  staffingErrors.add(consultantsRes.status !== 200);
  check(consultantsRes, { 'consultants list is 200': (r) => r.status === 200 });

  sleep(1);
}
