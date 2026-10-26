// ============================================================
// 邻里鲜生 · 下单并发压测（零超卖）
//
// 前置条件（A 交付后可用）：
//   1. POST /api/v1/auth/login 可用，返回令牌
//   2. POST /api/v1/orders 可用，支持 Idempotency-Key
//   3. 商品库存充足且为同一商品（热点行竞争）
//
// 运行：
//   k6 run -e PHONE=13800000003 -e PASSWORD=123456 test/k6/order-concurrency.js
//
// 通过条件：
//   - 下单 P95 ≤ 800ms、P99 ≤ 1.5s（需求 7.1.1）
//   - 峰值 ≥ 200 TPS
//   - 结束后库存恒等式成立：总 = 可用 + 锁定 + 已售，且无负值
//     （用 test/k6/check-stock.sql 核对，结果为空即通过）
// ============================================================
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const PHONE = __ENV.PHONE || '13800000003';
const PASSWORD = __ENV.PASSWORD || '123456';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || 1);
const TARGET_TPS = Number(__ENV.TARGET_TPS || 200);
const HOLD_SECONDS = Number(__ENV.HOLD_SECONDS || 30);

const accepted = new Counter('order_accepted');
const rejectedStock = new Counter('order_rejected_stock');
const orderLatency = new Trend('order_latency', true);
const bizFailRate = new Rate('order_biz_fail');

export const options = {
  scenarios: {
    order_burst: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PRE_VUS || 100),
      maxVUs: Number(__ENV.MAX_VUS || 400),
      stages: [
        { target: TARGET_TPS, duration: '30s' },
        { target: TARGET_TPS, duration: `${HOLD_SECONDS}s` },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    'order_latency': ['p(95)<800', 'p(99)<1500'],
    'order_biz_fail': ['rate<0.01'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ phone: PHONE, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  );

  check(res, { '登录成功': (r) => r.status === 200 });

  let token = '';
  try {
    token = res.json('data.token');
  } catch (e) {
    token = '';
  }

  if (!token) {
    throw new Error('登录未拿到 token，请确认 auth 接口已实现且测试账号可用');
  }
  return { token };
}

export default function (data) {
  const vu = exec.vu.idInTest;
  const iter = exec.vu.iterationInInstance;
  // 每次请求唯一幂等键：同一请求重试仍可复用该值
  const idemKey = `k6-${vu}-${iter}-${Date.now()}`;

  const body = JSON.stringify({
    items: [{ productId: PRODUCT_ID, quantity: 1 }],
    deliveryType: 1,
  });

  const res = http.post(`${BASE_URL}/api/v1/orders`, body, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,
      'Idempotency-Key': idemKey,
    },
    tags: { name: 'POST /api/v1/orders' },
  });

  orderLatency.add(res.timings.duration);

  let code = -1;
  try {
    code = res.json('code');
  } catch (e) {
    code = -1;
  }

  if (code === 0) {
    accepted.add(1);
    bizFailRate.add(false);
  } else if (code === 20003) {
    rejectedStock.add(1);
    bizFailRate.add(false); // 库存不足属于预期结果，不计入失败
  } else {
    bizFailRate.add(true);
  }

  check(res, {
    '业务码为 0 或 20003': () => code === 0 || code === 20003,
  });
}

export function teardown() {
  console.log('压测结束，请执行库存恒等式核对：');
  console.log('  docker exec -i linli-mysql mysql -uroot -proot123456 linli_fresh < test/k6/check-stock.sql');
}
