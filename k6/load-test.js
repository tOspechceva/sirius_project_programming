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
        mixed_api_load: {
            executor: "ramping-vus",
            exec: "mixedFlow",
            startVUs: profile.startVus,
            stages: profile.stages,
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

export function mixedFlow() {
    const shouldPost = Math.random() < profile.postRatio;

    if (shouldPost) {
        const response = http.post(
            `${profile.baseUrl}/api/users`,
            buildUniqueUserPayload(),
            {
                headers: { "Content-Type": "application/json" },
                tags: { endpoint: "users_create", method: "POST" }
            }
        );

        postReqCount.add(1);
        postReqDuration.add(response.timings.duration);
        check(response, {
            "POST /api/users status is 201": (r) => r.status === 201
        });
    } else {
        const response = http.get(`${profile.baseUrl}/api/progress/users`, {
            tags: { endpoint: "progress_stats", method: "GET" }
        });

        getReqCount.add(1);
        getReqDuration.add(response.timings.duration);
        check(response, {
            "GET /api/progress/users status is 200": (r) => r.status === 200
        });
    }

    sleep(Number(__ENV.SLEEP_SECONDS || 0.2));
}
