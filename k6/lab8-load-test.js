import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// -----------------------------
// Config from environment
// -----------------------------
const CRUD_BASE_URL = __ENV.CRUD_BASE_URL || "http://app:8081";
const ADDITIONAL_BASE_URL = __ENV.ADDITIONAL_BASE_URL || "http://additional-service:8082";

const targetVus = Number(__ENV.TARGET_VUS || 10);
const startVus = Number(__ENV.START_VUS || 1);
const postPoolRatio = Number(__ENV.POST_POOL_RATIO || 0.5);

const rampUp = __ENV.RAMP_UP || "20s";
const steady = __ENV.STEADY_DURATION || "40s";
const rampDown = __ENV.RAMP_DOWN || "15s";

const sleepSeconds = Number(__ENV.SLEEP_SECONDS || 0.2);
const httpTimeout = __ENV.HTTP_TIMEOUT || "120s";

const boundedPostRatio = Math.min(0.9, Math.max(0.1, postPoolRatio));
const postTargetVus = Math.max(0, Math.min(targetVus, Math.round(targetVus * boundedPostRatio)));
const getTargetVus = targetVus - postTargetVus;

const postStartVUs = Math.max(0, Math.min(startVus, Math.round(startVus * boundedPostRatio)));
const getStartVUs = startVus - postStartVUs;

// -----------------------------
// Metrics (names match existing chart scripts)
// -----------------------------
const postReqDuration = new Trend("post_req_duration");
const getReqDuration = new Trend("get_req_duration");

const postReqCount = new Counter("post_req_count");
const getReqCount = new Counter("get_req_count");

function stagesFor(vus) {
  return [
    { duration: rampUp, target: vus },
    { duration: steady, target: vus },
    { duration: rampDown, target: 0 },
  ];
}

function buildUniqueUserPayload() {
  const suffix = `${Date.now()}-${__VU}-${__ITER}`;
  return JSON.stringify({
    login: `perf_user_${suffix}`,
    email: `perf_${suffix}@example.com`,
    registrationDate: "2026-03-03",
  });
}

function countFromJson(body) {
  if (Array.isArray(body)) return body.length;
  if (body && Array.isArray(body.content)) return body.content.length;
  if (body && Array.isArray(body.data)) return body.data.length;
  return 0;
}

function timedGet(url, tags) {
  const res = http.get(url, {
    timeout: httpTimeout,
    tags: tags,
  });

  // Для snapshots главное — статус/кол-во/время.
  check(res, {
    [`GET ${url} status is 200`]: (r) => r.status === 200,
  });

  let json = null;
  try {
    json = res.json();
  } catch (e) {
    // Если тело не JSON (или парсинг упал) — просто не сможем посчитать count.
  }

  const count = countFromJson(json);
  return { status: res.status, count: count, durationMs: res.timings.duration };
}

// -----------------------------
// Snapshots before/after run
// -----------------------------
export function setup() {
  const startUsers = timedGet(`${CRUD_BASE_URL}/api/users`, {
    snapshot: "start",
    type: "users",
  });
  // "Прогресс" в LAB8 — это endpoint additional-service:
  // GET /api/progress/users (прогресс в процентах по всем пользователям).
  const startProgress = timedGet(`${ADDITIONAL_BASE_URL}/api/progress/users`, {
    snapshot: "start",
    type: "progress",
  });

  console.log(
    `SNAPSHOT_START usersCount=${startUsers.count} usersDurationMs=${startUsers.durationMs} progressCount=${startProgress.count} progressDurationMs=${startProgress.durationMs}`
  );

  return { startUsers, startProgress };
}

export function teardown(data) {
  const endUsers = timedGet(`${CRUD_BASE_URL}/api/users`, {
    snapshot: "end",
    type: "users",
  });
  const endProgress = timedGet(`${ADDITIONAL_BASE_URL}/api/progress/users`, {
    snapshot: "end",
    type: "progress",
  });

  console.log(
    `SNAPSHOT_END usersCount=${endUsers.count} usersDurationMs=${endUsers.durationMs} progressCount=${endProgress.count} progressDurationMs=${endProgress.durationMs}`
  );

  // Чтобы было наглядно, выводим разницу относительно start.
  if (data && data.startUsers && data.startProgress) {
    console.log(
      `SNAPSHOT_DELTA usersCountDelta=${endUsers.count - data.startUsers.count} progressCountDelta=${endProgress.count - data.startProgress.count}`
    );
  }
}

// -----------------------------
// Load scenarios (LAB8 style)
// -----------------------------
export const options = {
  scenarios: {
    post_pool: {
      executor: "ramping-vus",
      exec: "postFlow",
      startVUs: postStartVUs,
      stages: stagesFor(postTargetVus),
      gracefulRampDown: "5s",
    },
    get_pool: {
      executor: "ramping-vus",
      exec: "getFlow",
      startVUs: getStartVUs,
      stages: stagesFor(getTargetVus),
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"],
  },
};

export function postFlow() {
  const response = http.post(
    `${CRUD_BASE_URL}/api/users`,
    buildUniqueUserPayload(),
    {
      timeout: httpTimeout,
      headers: { "Content-Type": "application/json" },
      tags: { endpoint: "users_create", method: "POST", pool: "post" },
    }
  );

  postReqCount.add(1);
  postReqDuration.add(response.timings.duration);
  check(response, {
    "POST /api/users status is 201": (r) => r.status === 201,
  });

  sleep(sleepSeconds);
}

export function getFlow() {
  const response = http.get(`${ADDITIONAL_BASE_URL}/api/progress/users`, {
    timeout: httpTimeout,
    tags: { endpoint: "progress_stats", method: "GET", pool: "get" },
  });

  getReqCount.add(1);
  getReqDuration.add(response.timings.duration);
  check(response, {
    "GET /api/progress/users status is 200": (r) => r.status === 200,
  });

  sleep(sleepSeconds);
}

