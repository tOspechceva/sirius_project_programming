export function readProfile() {
    const targetVus = Number(__ENV.TARGET_VUS || 10);
    const startVus = Number(__ENV.START_VUS || 1);
    const postPoolRatio = Number(__ENV.POST_POOL_RATIO || 0.5);
    const rampUp = __ENV.RAMP_UP || "20s";
    const steady = __ENV.STEADY_DURATION || "40s";
    const rampDown = __ENV.RAMP_DOWN || "15s";
    const boundedPostRatio = Math.min(0.9, Math.max(0.1, postPoolRatio));
    const postTargetVus = Math.max(0, Math.min(targetVus, Math.round(targetVus * boundedPostRatio)));
    const getTargetVus = targetVus - postTargetVus;
    const postStartVus = Math.max(0, Math.min(startVus, Math.round(startVus * boundedPostRatio)));
    const getStartVus = startVus - postStartVus;

    return {
        baseUrl: __ENV.BASE_URL || "http://localhost:8082",
        startVus,
        targetVus,
        postTargetVus,
        getTargetVus,
        postStartVus,
        getStartVus,
        stagesFor: (vus) => [
            { duration: rampUp, target: vus },
            { duration: steady, target: vus },
            { duration: rampDown, target: 0 }
        ]
    };
}
