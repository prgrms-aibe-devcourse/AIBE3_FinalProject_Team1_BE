package com.back.domain.chat.controller;

import com.back.config.TestConfig;
import com.back.domain.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.repository.ChatMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Sql("/sql/chat.sql")
@Sql(scripts = "/sql/clean-up.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 생성 성공")
    void createChatRoom_success() throws Exception {
        // given
        Long postId = 4L; // 아직 user1과 채팅방이 없는 게시글
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(postId);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("채팅방이 생성되었습니다."))
                .andExpect(jsonPath("$.data.chatRoomId").exists());
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("이미 존재하는 채팅방일 때")
    void createChatRoom_alreadyExists() throws Exception {
        // given
        Long postId = 1L; // 이미 user1-user2 채팅방이 존재하는 게시글
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(postId);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 채팅방입니다."))
                .andExpect(jsonPath("$.data.chatRoomId").exists());
    }

    @Test
    @DisplayName("로그인 안 한 상태에서 채팅방 생성 시도")
    void createChatRoom_unauthorized() throws Exception {
        // given
        Long postId = 4L;
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(postId);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("본인과 채팅방 생성 시도 - 예외 발생")
    void createChatRoom_withSelf_shouldThrow() throws Exception {
        // given
        Long postId = 5L; // user1 본인이 작성한 게시글
        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(postId);

        // when
        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("본인과 채팅방을 만들 수 없습니다."));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 검색어 없음")
    void getMyChatRooms_withoutKeyword() throws Exception {
        // given
        int totalChatRooms = 3; // user1이 참여 중인 전체 채팅방 수

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(totalChatRooms));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 게시글 제목으로 검색")
    void getMyChatRooms_searchByPostTitle_tent() throws Exception {
        // given
        String keyword = "텐트";
        String expectedPostTitle = "캠핑 텐트 대여";
        String expectedOtherNickname = "kim"; // user2

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", keyword))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].post.title").value(expectedPostTitle))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value(expectedOtherNickname))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 상대방 닉네임으로 검색")
    void getMyChatRooms_searchByMemberNickname() throws Exception {
        // given
        String keyword = "kim"; // user2의 닉네임
        String expectedPostTitle = "캠핑 텐트 대여";
        String expectedOtherNickname = "kim";

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", keyword))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].post.title").value(expectedPostTitle))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value(expectedOtherNickname))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 페이징 테스트")
    void getMyChatRooms_pagination() throws Exception {
        // given
        int totalChatRooms = 3;
        int pageSize = 2;

        // when - 첫 페이지
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", String.valueOf(pageSize)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(pageSize))
                .andExpect(jsonPath("$.data.page.totalElements").value(totalChatRooms))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(false))
                .andExpect(jsonPath("$.data.page.hasNext").value(true));

        // when - 두 번째 페이지
        ResultActions resultActions2 = mvc.perform(get("/api/v1/chats")
                        .param("page", "1")
                        .param("size", String.valueOf(pageSize)))
                .andDo(print());

        // then
        resultActions2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.page").value(1))
                .andExpect(jsonPath("$.data.page.size").value(pageSize))
                .andExpect(jsonPath("$.data.page.totalElements").value(totalChatRooms))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.first").value(false))
                .andExpect(jsonPath("$.data.page.last").value(true))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 상세 정보 조회 성공")
    void getChatRoom_success() throws Exception {
        // given
        Long chatRoomId = 1L; // user1 <-> user2, "캠핑 텐트 대여"
        Long expectedOtherMemberId = 2L; // user2
        String expectedOtherNickname = "kim";
        String expectedPostTitle = "캠핑 텐트 대여";

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", chatRoomId))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("채팅방 정보"))
                .andExpect(jsonPath("$.data.id").value(chatRoomId))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.post.title").value(expectedPostTitle))
                .andExpect(jsonPath("$.data.otherMember.id").value(expectedOtherMemberId))
                .andExpect(jsonPath("$.data.otherMember.nickname").value(expectedOtherNickname));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("존재하지 않는 채팅방 조회 시도")
    void getChatRoom_notFound() throws Exception {
        // given
        Long nonExistentChatRoomId = 99999L;

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", nonExistentChatRoomId))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."));
    }

    @Test
    @WithUserDetails(value = "user2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("권한 없는 채팅방 조회 시도")
    void getChatRoom_forbidden() throws Exception {
        // given
        Long chatRoomId = 2L; // user1 <-> user3 채팅방 (user2는 권한 없음)

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", chatRoomId))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.msg").value("해당 채팅방에 접근할 수 없습니다."));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 내 메시지 목록 조회 성공")
    void getChatRoomMessages_success() throws Exception {
        // given
        Long chatRoomId = 1L; // user1 <-> user2, 메시지 3개 존재

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("해당 채팅방 내 메세지 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].content").value("메시지 3"))
                .andExpect(jsonPath("$.data.content[1].content").value("메시지 2"))
                .andExpect(jsonPath("$.data.content[2].content").value("메시지 1"));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("읽음 처리 성공 - lastMessageId 업데이트")
    void markAsRead_success() throws Exception {
        // given
        Long chatRoomId = 1L; // user1 <-> user2
        Long lastMessageId = 3L; // 읽음 처리할 마지막 메시지 ID
        Long myId = 1L; // user1

        // when
        ResultActions resultActions = mvc.perform(patch("/api/v1/chats/{chatRoomId}/read", chatRoomId)
                        .param("lastMessageId", String.valueOf(lastMessageId)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("읽음 처리 완료"));

        ChatMember chatMember = chatMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, myId).orElseThrow();
        assertEquals(lastMessageId, chatMember.getLastReadMessageId());
    }
}