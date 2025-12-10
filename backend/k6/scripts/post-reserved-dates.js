import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = "http://host.docker.internal:8080";

export const options = {
    vus: 30,
    duration: "30s",
    tags: { testid: `reserved-dates-${Date.now()}` },
    summaryTrendStats: ["min", "avg", "max", "p(90)", "p(95)", "p(99)"],
    thresholds: {
        http_req_duration: ["p(95)<500"],
        http_req_failed: ["rate<0.1"],
    }
};

export default function () {
    const postId = Math.floor(Math.random() * 100) + 1;

    const res = http.get(`${BASE_URL}/api/v1/posts/${postId}/reserved-dates`, {
        tags: { name: "GET /api/v1/posts/:id/reserved-dates" }
    });

    check(res, {
        "예약 날짜 OK": r => r.status === 200
    });

    sleep(0.1);
}