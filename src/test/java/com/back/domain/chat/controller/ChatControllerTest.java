package com.back.domain.chat.controller;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.chat.service.ChatService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.common.ReceiveMethod;
import com.back.domain.post.common.ReturnMethod;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ChatService chatService;

    private Member member1;
    private Member member2;
    private Member member3;
    private Member member4;
    private Post post1;
    private Post post2;
    private Post post3;

    @BeforeEach
    void setUp() {
        chatRoomRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();

        // 회원 생성
        member1 = memberRepository.save(new Member(
                "user1@test.com", "1234", "홍길동", "010-1111-1111",
                "서울시 강남구", "테헤란로 123", "hong"));

        member2 = memberRepository.save(new Member(
                "user2@test.com", "1234", "김철수", "010-2222-2222",
                "서울시 서초구", "서초대로 456", "kim"));

        member3 = memberRepository.save(new Member(
                "user3@test.com", "1234", "이영희", "010-3333-3333",
                "서울시 마포구", "월드컵북로 789", "lee"));

        member4 = memberRepository.save(new Member(
                "user4@test.com", "1234", "박민수", "010-4444-4444",
                "서울시 송파구", "잠실로 101", "park"));

        // 게시글 생성
        Category category = Category.create("노트북", null);
        categoryRepository.save(category);

        post1 = postRepository.save(new Post(
                "캠핑 텐트 대여",
                "4인용 텐트입니다.",
                ReceiveMethod.DELIVERY,
                ReturnMethod.DELIVERY,
                null,
                null,
                10000,
                5000,
                false,
                member2,
                new ArrayList<>(),   // options
                new ArrayList<>(),   // images
                new ArrayList<>(),   // postRegions
                category
        ));

        post2 = postRepository.save(new Post(
                "노트북 대여합니다",
                "맥북 프로입니다.",
                ReceiveMethod.DIRECT,
                ReturnMethod.DIRECT,
                null,
                null,
                50000,
                20000,
                false,
                member3,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                category
        ));

        post3 = postRepository.save(new Post(
                "카메라 렌탈",
                "소니 A7III입니다.",
                ReceiveMethod.ANY,
                ReturnMethod.ANY,
                null,
                null,
                30000,
                15000,
                false,
                member4,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                category
        ));

        // member1이 여러 채팅방 생성
        chatService.createOrGetChatRoom(post1.getId(), member1.getId());  // member1 <-> member2
        chatService.createOrGetChatRoom(post2.getId(), member1.getId());  // member1 <-> member3
        chatService.createOrGetChatRoom(post3.getId(), member1.getId());  // member1 <-> member4
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 생성 성공")
    void test1_createChatRoom_success() throws Exception {
        chatRoomRepository.deleteAll();

        // given
        Long postId = post1.getId();
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
    void test2_createChatRoom_alreadyExists() throws Exception {
        // given
        Long postId = post1.getId();
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
    void test3_createChatRoom_unauthorized() throws Exception {
        // given
        Long postId = post1.getId();
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

//    @Test
//    @WithUserDetails(value = "user2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
//    @DisplayName("본인과 채팅방 생성 시도 - 예외 발생")
//    void test4_createChatRoom_withSelf_shouldThrow() throws Exception {
//        // given
//        Long postId = post1.getId();
//        CreateChatRoomReqBody reqBody = new CreateChatRoomReqBody(postId);
//
//        // when
//        ResultActions resultActions = mvc.perform(post("/api/v1/chats")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(reqBody)))
//                .andDo(print());
//
//        // then
//        resultActions
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.status").value(400))
//                .andExpect(jsonPath("$.msg").value("본인과 채팅방을 만들 수 없습니다."));
//    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 검색어 없음")
    void test5_getMyChatRooms_withoutKeyword() throws Exception {
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
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(10))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 게시글 제목으로 검색 (텐트)")
    void test6_getMyChatRooms_searchByPostTitle_tent() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "텐트"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value("kim"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 게시글 제목으로 검색 (대여)")
    void test7_getMyChatRooms_searchByPostTitle_rent() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "대여"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 상대방 닉네임으로 검색")
    void test8_getMyChatRooms_searchByMemberNickname() throws Exception {
        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "kim"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("내 채팅방 목록"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].otherMember.nickname").value("kim"))
                .andExpect(jsonPath("$.data.content[0].post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 페이징 테스트")
    void test9_getMyChatRooms_pagination() throws Exception {
        // 첫 페이지 (size=2)
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(false))
                .andExpect(jsonPath("$.data.page.hasNext").value(true));

        // 두 번째 페이지
        ResultActions resultActions2 = mvc.perform(get("/api/v1/chats")
                        .param("page", "1")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.page").value(1))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.page.first").value(false))
                .andExpect(jsonPath("$.data.page.last").value(true))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 상세 정보 조회 성공")
    void test10_getChatRoom_success() throws Exception {
        // given - member1과 member2의 채팅방
        Long chatRoomId = chatService.createOrGetChatRoom(post1.getId(), member1.getId()).chatRoomId();

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
                .andExpect(jsonPath("$.data.post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.data.otherMember.id").value(member2.getId()))
                .andExpect(jsonPath("$.data.otherMember.nickname").value("kim"));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("존재하지 않는 채팅방 조회 시도")
    void test11_getChatRoom_notFound() throws Exception {
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
    @DisplayName("권한 없는 채팅방 조회 시도 - member2가 member1-member3 채팅방 접근")
    void test12_getChatRoom_forbidden() throws Exception {
        // given
        Long chatRoomId = chatService.createOrGetChatRoom(post2.getId(), member1.getId()).chatRoomId();

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
    @WithUserDetails(value = "user2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 상세 조회 - 상대방 관점에서 확인")
    void test13_getChatRoom_fromOtherMemberPerspective() throws Exception {
        // given
        Long chatRoomId = chatService.createOrGetChatRoom(post1.getId(), member1.getId()).chatRoomId();

        // when
        ResultActions resultActions = mvc.perform(get("/api/v1/chats/{chatRoomId}", chatRoomId))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(chatRoomId))
                .andExpect(jsonPath("$.data.post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.data.otherMember.id").value(member1.getId()))
                .andExpect(jsonPath("$.data.otherMember.nickname").value("hong"));
    }

}