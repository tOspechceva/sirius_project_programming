import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { readProfile } from "./profile.js";

const profile = readProfile();
const PROXY_BASE_URL = __ENV.PROXY_BASE_URL || "http://127.0.0.1:18081";
const ADDITIONAL_BASE_URL = __ENV.ADDITIONAL_BASE_URL || "http://10.60.3.36:8083";
const HTTP_TIMEOUT = __ENV.HTTP_TIMEOUT || "120s";
const SLEEP_SECONDS = Number(__ENV.SLEEP_SECONDS || 0.2);

const postReqDuration = new Trend("post_req_duration");
const getReqDuration = new Trend("get_req_duration");
const postReqCount = new Counter("post_req_count");
const getReqCount = new Counter("get_req_count");

function buildKafkaUserCommand() {
  const suffix = `${Date.now()}-${__VU}-${__ITER}`;
  return JSON.stringify({
    entity: "USER",
    operation: "POST",
    payload: {
      login: `k6_proxy_${suffix}`,
      email: `k6_proxy_${suffix}@example.com`,
      registrationDate: "2024-06-01",
    },
  });
}

export const options = {
  scenarios: {
    post_pool: {
      executor: "ramping-vus",
      exec: "postFlow",
      startVUs: profile.postStartVus,
      stages: profile.stagesFor(profile.postTargetVus),
      gracefulRampDown: "5s",
    },
    get_pool: {
      executor: "ramping-vus",
      exec: "getFlow",
      startVUs: profile.getStartVus,
      stages: profile.stagesFor(profile.getTargetVus),
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"],
  },
};

export function postFlow() {
  const response = http.post(`${PROXY_BASE_URL}/publish`, buildKafkaUserCommand(), {
    timeout: HTTP_TIMEOUT,
    headers: { "Content-Type": "application/json" },
    tags: { endpoint: "proxy_publish", method: "POST", pool: "post" },
  });

  postReqCount.add(1);
  postReqDuration.add(response.timings.duration);
  check(response, {
    "POST /publish status is 200": (r) => r.status === 200,
  });

  sleep(SLEEP_SECONDS);
}

export function getFlow() {
  const response = http.get(`${ADDITIONAL_BASE_URL}/api/progress/users`, {
    timeout: HTTP_TIMEOUT,
    tags: { endpoint: "progress_stats", method: "GET", pool: "get" },
  });

  getReqCount.add(1);
  getReqDuration.add(response.timings.duration);
  check(response, {
    "GET /api/progress/users status is 200": (r) => r.status === 200,
  });

  sleep(SLEEP_SECONDS);
}
