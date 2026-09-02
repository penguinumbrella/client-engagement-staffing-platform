import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = 'http://api-gateway-test-alb-568701708.us-east-1.elb.amazonaws.com';

const loginErrors = new Rate('login_errors');
const loginDuration = new Trend('login_duration');
const engagementErrors = new Rate('engagement_errors');
const engagementDuration = new Trend('engagement_duration');

export const options = {
  scenarios: {
    ramping_load: {
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
  },
};

export default function () {
  // Real DB read + JWT signing work
  const loginRes = http.post(
    `${BASE_URL}/auth/api/auth/login`,
    JSON.stringify({
      email: 'test.manager3@skillstorm-test.com',
      password: 'TestPassword123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  loginDuration.add(loginRes.timings.duration);
  loginErrors.add(loginRes.status !== 200);
  check(loginRes, { 'login status is 200': (r) => r.status === 200 });

  // Lightweight gateway-routing-only path (expected 401, no auth needed)
  const engagementRes = http.get(`${BASE_URL}/engagement/api/engagements`);
  engagementDuration.add(engagementRes.timings.duration);
  engagementErrors.add(engagementRes.status !== 401 && engagementRes.status !== 200);
  check(engagementRes, { 'engagement status is 401 or 200': (r) => r.status === 401 || r.status === 200 });

  sleep(1);
}
