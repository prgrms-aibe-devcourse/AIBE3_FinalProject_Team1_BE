import http from "k6/http";
import { check } from "k6";

const BASE_URL = "http://host.docker.internal:8080";

export const options = {
    vus: 30,
    duration: "30s",
    tags: { testid: `reserved-dates-${Date.now()}` }
};

export default function () {
    const postId = Math.floor(Math.random() * 100) + 1;

    const res = http.get(`${BASE_URL}/api/v1/posts/${postId}/reserved-dates`, {
        tags: {
            name: "GET /api/v1/posts/:id/reserved-dates",
            postId: postId
        }
    });

    check(res, { "예약 날짜 OK": r => r.status === 200 });
}
