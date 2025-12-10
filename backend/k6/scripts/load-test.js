import http from "k6/http";
import { check, sleep, group } from "k6";
import { Rate, Trend } from "k6/metrics";

const errorRate = new Rate("errors");
const postDetailDuration = new Trend("post_detail_duration");
const reservationDuration = new Trend("reservation_duration");

export const options = {
    stages: [
        { duration: "30s", target: 10 },
        { duration: "1m", target: 50 },
        { duration: "30s", target: 100 },
        { duration: "1m", target: 100 },
        { duration: "30s", target: 0 },
    ],
    thresholds: {
        http_req_duration: ["p(95)<500", "p(99)<1200"],
        http_req_failed: ["rate<0.1"],
        errors: ["rate<0.1"],
        post_detail_duration: ["p(95)<300"],
        reservation_duration: ["p(95)<400"],
    },
    tags: { testid: `load-test-${Date.now()}` },
    summaryTrendStats: ["min", "avg", "max", "p(90)", "p(95)", "p(99)"],
};

const BASE_URL = "http://host.docker.internal:8080";

// 테스트용 임시 유저 생성
const TEST_USER = {
    email: `test${Date.now()}@example.com`,
    password: "testpassword123",
    nickname: `tester${Date.now()}`,
};

export function setup() {
    console.log("Setup: 테스트 계정 생성 및 로그인");

    // 회원가입
    const signupRes = http.post(
        `${BASE_URL}/api/v1/members`,
        JSON.stringify(TEST_USER),
        { headers: { "Content-Type": "application/json" } }
    );

    if (signupRes.status !== 201) {
        console.error("회원가입 실패:", signupRes.status, signupRes.body);
    }

    // 로그인
    const loginRes = http.post(
        `${BASE_URL}/api/v1/members/login`,
        JSON.stringify({
            email: TEST_USER.email,
            password: TEST_USER.password,
        }),
        { headers: { "Content-Type": "application/json" } }
    );

    if (loginRes.status !== 200) {
        console.error("로그인 실패:", loginRes.status, loginRes.body);
    }

    const token = loginRes.cookies.accessToken?.[0]?.value ?? null;

    console.log("로그인 토큰:", token ? "발급 성공" : "발급 실패");

    return { accessToken: token };
}

export default function (data) {
    const token = data.accessToken;

    const headers = {
        "Content-Type": "application/json",
        Cookie: token ? `accessToken=${token}` : "",
    };

    let randomPostId = null;

    // 1. 게시글 목록 조회
    group("게시글 목록 조회", () => {
        const res = http.get(`${BASE_URL}/api/v1/posts?page=0&size=30`, {
            tags: { name: "GET /api/v1/posts" },
        });

        const success = check(res, {
            "게시글 목록 조회 성공": (r) => r.status === 200,
        });

        if (!success) {
            errorRate.add(1);
            console.error("게시글 목록 조회 실패:", res.status);
        } else {
            errorRate.add(0);
        }

        if (res.status === 200) {
            try {
                const body = res.json();
                const posts = body?.data?.content || [];
                if (posts.length > 0) {
                    randomPostId = posts[Math.floor(Math.random() * posts.length)].id;
                }
            } catch (e) {
                console.error("게시글 목록 파싱 실패:", e);
            }
        }
    });

    sleep(1);

    // 2. 게시글 상세 조회
    if (randomPostId) {
        group("게시글 상세 조회", () => {
            const res = http.get(`${BASE_URL}/api/v1/posts/${randomPostId}`, {
                tags: { name: "GET /api/v1/posts/:id" },
            });

            postDetailDuration.add(res.timings.duration);

            const success = check(res, {
                "게시글 상세 조회 성공": (r) => r.status === 200,
            });

            errorRate.add(success ? 0 : 1);
        });

        sleep(1);

        // 3. 후기 조회
        group("후기 조회", () => {
            const res = http.get(
                `${BASE_URL}/api/v1/posts/${randomPostId}/reviews?page=0&size=10`,
                {
                    tags: { name: "GET /api/v1/posts/:id/reviews" },
                }
            );

            const success = check(res, {
                "후기 조회 성공": (r) => r.status === 200,
            });

            errorRate.add(success ? 0 : 1);
        });

        sleep(1);

        // 4. 예약 날짜 조회
        group("예약 날짜 조회", () => {
            const res = http.get(
                `${BASE_URL}/api/v1/posts/${randomPostId}/reserved-dates`,
                {
                    tags: { name: "GET /api/v1/posts/:id/reserved-dates" },
                }
            );

            const success = check(res, {
                "예약 날짜 조회 성공": (r) => r.status === 200,
            });

            errorRate.add(success ? 0 : 1);
        });

        sleep(1);

        // 5. 즐겨찾기 토글
        if (token) {
            group("즐겨찾기 토글", () => {
                const res = http.post(
                    `${BASE_URL}/api/v1/posts/favorites/${randomPostId}`,
                    null,
                    {
                        headers,
                        tags: { name: "POST /api/v1/posts/favorites/:id" },
                    }
                );

                const success = check(res, {
                    "즐겨찾기 토글 성공": (r) => r.status === 200,
                });

                errorRate.add(success ? 0 : 1);
            });
        }
    }

    sleep(1);

    // 6. 일반 게시글 검색
    group("일반 검색 API", () => {
        const keywords = ["카메라", "렌즈", "캠핑", "드릴", "우쿨렐레", "청소기", "골프"];
        const keyword = keywords[Math.floor(Math.random() * keywords.length)];

        const res = http.get(
            `${BASE_URL}/api/v1/posts?page=0&size=20&keyword=${encodeURIComponent(keyword)}`,
            {
                tags: { name: "GET /api/v1/posts?keyword" },
            }
        );

        check(res, {
            "일반 검색 성공": (r) => r.status === 200
        });

        errorRate.add(res.status === 200 ? 0 : 1);
    });

    sleep(1);

    // 7. 채팅방 조회
    if (token) {
        group("채팅방 조회", () => {
            const res = http.get(`${BASE_URL}/api/v1/chats?page=0&size=10`, {
                headers,
                tags: { name: "GET /api/v1/chats" },
            });

            const success = check(res, {
                "채팅방 조회 성공": (r) => r.status === 200,
            });

            errorRate.add(success ? 0 : 1);
        });
    }

    sleep(1);
}

export function teardown(data) {
    console.log("테스트 종료");
    console.log("최종 토큰 상태:", data.accessToken ? "유효" : "없음");
}