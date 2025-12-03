package com.back.global.app.mcp.tool;

import com.back.domain.reservation.repository.ReservationQueryRepository;
import com.back.global.app.mcp.dto.CategoryStatsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticTools {

    @Value("${slack.user-token}")
    private String slackUserToken;

    @Value("${slack.channel-id}")
    private String slackChannelId;

    @Value("${slack.claude-member-id}")
    private String slackClaudeMemberId;

    private final ObjectMapper objectMapper;
    private final ReservationQueryRepository reservationQueryRepository;

    @Tool(description = """
            P2P 대여 서비스의 두 기간 동안 발생한 대여 거래를 카테고리별로 비교 분석합니다.
                - 통계는 플랫폼에서 발생한 '대여 거래'를 집계한 것입니다
                - 각 카테고리는 대여된 물품/서비스의 종류를 나타냅니다
                - 금액은 대여료(렌탈비)를 의미합니다
            
                ISO-8601 날짜 형식(YYYY-MM-DD)으로 기간을 지정하세요.
                사용자가 자연어(이번주, 지난주 등)로 요청하면 오늘 날짜를 기준으로 계산하여 변환하세요.
            
                응답 시 다음 사항을 명확히 해주세요:
                1. 이것은 '플랫폼 대여 거래 통계'임을 명시
                2. '지출'이 아닌 '대여료', '거래액' 용어 사용
                3. '소비'가 아닌 '대여', '이용' 용어 사용
                4. 대여 서비스 관점에서 인사이트 제공 (예: 인기 카테고리, 대여 패턴 등)
            """)
    public String compareCategoryStats(
            @ToolParam(description = """
                    첫 번째 비교 기간의 시작 날짜 (ISO-8601 형식: YYYY-MM-DD)
                    
                    사용자가 자연어로 요청한 경우 오늘을 기준으로 다음과 같이 변환하세요:
                    - '오늘': 오늘 날짜
                    - '어제': 어제 날짜
                    - '이번주': 이번 주 월요일
                    - '지난주': 지난 주 월요일
                    - '이번달': 이번 달 1일
                    - '지난달': 지난 달 1일
                    """)
            LocalDate firstPeriod,

            @ToolParam(description = """
                    두 번째 비교 기간의 종료 날짜 (ISO-8601 형식: YYYY-MM-DD)
                    
                    자연어 변환 규칙 (오늘 기준):
                    - '오늘', '어제': 해당 날짜
                    - '이번주': 오늘 날짜
                    - '지난주': 지난 주 일요일
                    - '이번달': 오늘 날짜
                    - '지난달': 지난 달 마지막 날
                    """)
            LocalDate secondPeriod)
    {
        try (Slack slack = Slack.getInstance()) {
            LocalDateTime from = firstPeriod.atStartOfDay();
            LocalDateTime to = secondPeriod.atTime(23, 59, 59);
            List<CategoryStatsDto> stats = reservationQueryRepository.getCategoryStats(from, to);

            // 메인 메시지 (스레드 시작점)
            ChatPostMessageResponse mainResponse = slack.methods(slackUserToken)
                                                        .chatPostMessage(request -> request
                                                                .channel(slackChannelId)
                                                                .text(String.format("📊 *카테고리별 통계 분석*\n기간: %s ~ %s", firstPeriod, secondPeriod))
                                                        );
            String threadTs = mainResponse.getTs();

            // 스레드에 내용 전달
            String jsonData = objectMapper.writeValueAsString(stats);
            String prompt = getPrompt(jsonData);

            slack.methods(slackUserToken)
                 .chatPostMessage(request -> request
                         .channel(slackChannelId)
                         .threadTs(threadTs)
                         .text(prompt)
                 );

            return "통계 데이터가 Claude에게 전달되었습니다.";
        } catch (Exception e) {
            log.error("카테고리 통계 Slack 전송 중 오류 발생", e);
            throw new RuntimeException(e);
        }
    }

    private String getPrompt(String jsonData) {
        return String.format(
                """
                <@%s> P2P 대여 플랫폼 카테고리별 통계 분석을 요청합니다.
                
                :warning: 중요 컨텍스트:
                • 이것은 대여 서비스 플랫폼의 거래 통계입니다
                • '지출/소비'가 아닌 '대여료/거래액', '대여/이용'으로 표현해주세요
                • 플랫폼 전체 거래 관점으로 분석해주세요
                • 응답 시 과도한 포맷팅(볼드, 특수문자)은 최소화해주세요
                
                :page_facing_up: 데이터 구조 설명:
                • categoryName: 대여 카테고리명
                • tradeCount: 해당 카테고리의 총 대여 거래 건수
                • totalFee: 해당 카테고리의 총 대여료 합계 (단위: 원)
                • 정렬 기준: tradeCount 내림차순, tradeCount가 같으면 totalFee 내림차순
                • 평균 거래액은 totalFee ÷ tradeCount로 계산 가능합니다
                
                :clipboard: 분석 요청사항:
                1. :bar_chart: 전체 대여 거래 현황 (총 대여료, 거래 건수)
                2. :chart_with_upwards_trend: 카테고리별 대여 트렌드 (인기 카테고리, 평균 거래액)
                3. :bulb: 플랫폼 인사이트 (대여 패턴, 시즌 트렌드 등)
                
                :paperclip: 첨부된 JSON 데이터:
                
                %s
                """,
                slackClaudeMemberId, jsonData
        );
    }
}
