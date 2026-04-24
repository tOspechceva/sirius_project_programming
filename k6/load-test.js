import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { readProfile } from "./profile.js";

const profile = readProfile();

const postReqDuration = new Trend("post_req_duration");
const getReqDuration = new Trend("get_req_duration");
const postReqCount = new Counter("post_req_count");
const getReqCount = new Counter("get_req_count");

export const options = {
    scenarios: {
        post_pool: {
            executor: "ramping-vus",
            exec: "postFlow",
            startVUs: profile.postStartVus,
            stages: profile.stagesFor(profile.postTargetVus),
            gracefulRampDown: "5s"
        },
        get_pool: {
            executor: "ramping-vus",
            exec: "getFlow",
            startVUs: profile.getStartVus,
            stages: profile.stagesFor(profile.getTargetVus),
            gracefulRampDown: "5s"
        }
    },
    thresholds: {
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<1500"]
    }
};

function buildUniqueUserPayload() {
    const suffix = `${Date.now()}-${__VU}-${__ITER}`;
    return JSON.stringify({
        login: `perf_user_${suffix}`,
        email: `perf_${suffix}@example.com`,
        registrationDate: "2026-03-03"
    });
}

export function postFlow() {
    const response = http.post(
        `${profile.baseUrl}/api/users`,
        buildUniqueUserPayload(),
        {
            headers: { "Content-Type": "application/json" },
            tags: { endpoint: "users_create", method: "POST", pool: "post" }
        }
    );

    postReqCount.add(1);
    postReqDuration.add(response.timings.duration);
    check(response, {
        "POST /api/users status is 201": (r) => r.status === 201
    });

    sleep(Number(__ENV.SLEEP_SECONDS || 0.2));
}

export function getFlow() {
    const response = http.get(`${profile.baseUrl}/api/progress/users`, {
            tags: { endpoint: "progress_stats", method: "GET", pool: "get" }
        });

    getReqCount.add(1);
    getReqDuration.add(response.timings.duration);
    check(response, {
        "GET /api/progress/users status is 200": (r) => r.status === 200
    });

    sleep(Number(__ENV.SLEEP_SECONDS || 0.2));
}
