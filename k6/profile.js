export function readProfile() {
    const targetVus = Number(__ENV.TARGET_VUS || 10);
    const startVus = Number(__ENV.START_VUS || 1);
    const postRatio = Number(__ENV.POST_RATIO || 0.5);
    const rampUp = __ENV.RAMP_UP || "20s";
    const steady = __ENV.STEADY_DURATION || "40s";
    const rampDown = __ENV.RAMP_DOWN || "15s";

    return {
        baseUrl: __ENV.BASE_URL || "http://localhost:8082",
        postRatio,
        startVus,
        targetVus,
        stages: [
            { duration: rampUp, target: targetVus },
            { duration: steady, target: targetVus },
            { duration: rampDown, target: 0 }
        ]
    };
}
