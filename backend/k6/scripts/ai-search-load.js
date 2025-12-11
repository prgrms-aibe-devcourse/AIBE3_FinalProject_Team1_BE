import http from "k6/http";
import {check, group, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";

// 커스텀 메트릭
const errorRate = new Rate("errors");
const aiSearchDuration = new Trend("ai_search_duration");

export const options = {
    stages: [
        {duration: "30s", target: 10},
        {duration: "1m", target: 50},
        {duration: "30s", target: 100},
        {duration: "1m", target: 50},
        {duration: "30s", target: 0},
    ],
    thresholds: {
        http_req_duration: ["p(95)<5000", "p(99)<10000"],  // AI 검색은 느릴 수 있으니 넉넉히
        http_req_failed: ["rate<0.1"],
        errors: ["rate<0.1"],
        ai_search_duration: ["p(95)<5000"],
    },
    tags: {testid: `ai-search-load-${Date.now()}`},
    summaryTrendStats: ["min", "avg", "max", "p(90)", "p(95)", "p(99)"],
};

const BASE_URL = "https://api.chwimeet.store";

// 테스트용 동적 계정
const TEST_USER = {
    email: `ai_test_${Date.now()}@example.com`,
    password: "testpassword123!",
    nickname: `aiTester${Date.now()}`,
};

export function setup() {
    console.log("⚙ Setup: 테스트용 계정 생성 및 로그인");

    // 회원가입
    const signupRes = http.post(
        `${BASE_URL}/api/v1/members`,
        JSON.stringify(TEST_USER),
        {headers: {"Content-Type": "application/json"}}
    );

    if (signupRes.status !== 201) {
        console.error("❌ 회원가입 실패:", signupRes.status, signupRes.body);
    }

    // 로그인
    const loginRes = http.post(
        `${BASE_URL}/api/v1/members/login`,
        JSON.stringify({
            email: TEST_USER.email,
            password: TEST_USER.password,
        }),
        {headers: {"Content-Type": "application/json"}}
    );

    if (loginRes.status !== 200) {
        console.error("❌ 로그인 실패:", loginRes.status, loginRes.body);
    }

    const token = loginRes.cookies.accessToken?.[0]?.value ?? null;
    console.log("🔑 로그인 토큰:", token ? "발급 성공" : "발급 실패");

    return {token};
}

export default function (data) {
    const headers = {
        Cookie: `accessToken=${data.token}`,
    };

    const queries = ["카메라", "렌즈", "캠핑", "드릴", "우쿨렐레", "자전거", "노트북"];
    const q = queries[Math.floor(Math.random() * queries.length)];

    group("🔍 AI 검색 API", () => {
        const res = http.get(
            `${BASE_URL}/api/v1/posts/search-ai?query=${encodeURIComponent(q)}`,
            {
                headers,
                tags: {name: "GET /api/v1/posts/search-ai"},
            }
        );

        // 커스텀 메트릭 기록
        aiSearchDuration.add(res.timings.duration);

        const success = check(res, {
            "AI 검색 성공": (r) => r.status === 200,
        });

        errorRate.add(success ? 0 : 1);
    });

    sleep(1);
}

export function teardown(data) {
    console.log("🧹 테스트 종료");
    console.log("최종 토큰 상태:", data.token ? "유효" : "없음");
}
