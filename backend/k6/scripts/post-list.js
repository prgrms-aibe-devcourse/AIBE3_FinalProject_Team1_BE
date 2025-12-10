import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = "http://host.docker.internal:8080";

export const options = {
    vus: 30,
    duration: "30s",
    tags: { testid: `posts-list-${Date.now()}` },
    summaryTrendStats: ["min", "avg", "max", "p(90)", "p(95)", "p(99)"],
    thresholds: {
        http_req_duration: ["p(95)<500"],
        http_req_failed: ["rate<0.1"],
    }
};

export default function () {
    const res = http.get(`${BASE_URL}/api/v1/posts?page=0&size=20`, {
        tags: { name: "GET /api/v1/posts" }
    });

    check(res, {
        "목록 조회 OK": r => r.status === 200
    });

    sleep(0.1);
}