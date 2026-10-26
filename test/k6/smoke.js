// ============================================================
// 邻里鲜生 · k6 冒烟压测（当前接口可用）
// 运行：k6 run test/k6/smoke.js
//       k6 run -e BASE_URL=http://127.0.0.1:8080 test/k6/smoke.js
// 判定：对齐《需求文档》7.1.1，读接口 P95 ≤ 300ms、错误率 < 1%
// ============================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';

const errorRate = new Rate('biz_error_rate');
const healthLatency = new Trend('health_p95', true);

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'biz_error_rate': ['rate<0.01'],
    'health_p95': ['p(95)<300'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const health = http.get(`${BASE_URL}/api/v1/health`, {
    tags: { name: 'GET /api/v1/health' },
  });
  healthLatency.add(health.timings.duration);

  let ok = check(health, {
    'HTTP 200': (r) => r.status === 200,
    '业务码 code=0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (e) {
        return false;
      }
    },
    '返回 traceId': (r) => {
      try {
        return !!r.json('traceId');
      } catch (e) {
        return false;
      }
    },
  });
  errorRate.add(!ok);

  const actuator = http.get(`${BASE_URL}/actuator/health`, {
    tags: { name: 'GET /actuator/health' },
  });
  check(actuator, { 'Actuator UP': (r) => r.status === 200 });

  const doc = http.get(`${BASE_URL}/api/v1/doc`, {
    tags: { name: 'GET /api/v1/doc' },
  });
  check(doc, { 'OpenAPI 可读': (r) => r.status === 200 });

  sleep(1);
}
