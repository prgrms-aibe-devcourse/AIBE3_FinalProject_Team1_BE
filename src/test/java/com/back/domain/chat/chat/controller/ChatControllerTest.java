package com.back.domain.chat.chat.controller;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.chat.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.chat.chat.service.ChatService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
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
        member1 = new Member("user1@test.com", "1234", "홍길동", "010-1111-1111",
                "서울시 강남구", "테헤란로 123", "hong");
        member2 = new Member("user2@test.com", "1234", "김철수", "010-2222-2222",
                "서울시 서초구", "서초대로 456", "kim");
        member3 = new Member("user3@test.com", "1234", "이영희", "010-3333-3333",
                "서울시 마포구", "월드컵북로 789", "lee");
        member4 = new Member("user4@test.com", "1234", "박민수", "010-4444-4444",
                "서울시 송파구", "잠실로 101", "park");

        // 게시글 생성
        Category category = categoryRepository.save(
                Category.create("노트북", null)
        );

        post1 = postRepository.save(Post.builder()
                .title("캠핑 텐트 대여")
                .content("4인용 텐트입니다.")
                .receiveMethod(ReceiveMethod.DELIVERY)
                .returnMethod(ReturnMethod.DELIVERY)
                .deposit(10000)
                .fee(5000)
                .author(member2)
                .category(category)
                .build()
        );

        post2 = postRepository.save(Post.builder()
                .title("노트북 대여합니다")
                .content("맥북 프로입니다.")
                .receiveMethod(ReceiveMethod.DIRECT)
                .returnMethod(ReturnMethod.DIRECT)
                .deposit(50000)
                .fee(20000)
                .author(member3)
                .category(category)
                .build()
        );

        post3 = postRepository.save(Post.builder()
                .title("카메라 렌탈")
                .content("소니 A7III입니다.")
                .receiveMethod(ReceiveMethod.ANY)
                .returnMethod(ReturnMethod.ANY)
                .deposit(30000)
                .fee(15000)
                .author(member4)
                .category(category)
                .build()
        );

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
                .andExpect(jsonPath("$.message").value("채팅방이 생성되었습니다."))
                .andExpect(jsonPath("$.chatRoomId").exists());
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
                .andExpect(jsonPath("$.message").value("이미 존재하는 채팅방입니다."))
                .andExpect(jsonPath("$.chatRoomId").exists());
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
//                .andExpect(jsonPath("$.resultCode").value("400-1"))
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
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))  // 3개 채팅방
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(1));
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
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.content[0].otherMember.nickname").value("kim"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
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
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))  // "캠핑 텐트 대여", "노트북 대여합니다"
                .andExpect(jsonPath("$.page.totalElements").value(2));
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
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].otherMember.nickname").value("kim"))
                .andExpect(jsonPath("$.content[0].post.title").value("캠핑 텐트 대여"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithUserDetails(value = "user1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("채팅방 목록 조회 - 페이징 테스트")
    void test9_getMyChatRooms_pagination() throws Exception {
        // when - 첫 페이지 (size=2)
        ResultActions resultActions = mvc.perform(get("/api/v1/chats")
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.first").value(true))
                .andExpect(jsonPath("$.page.last").value(false))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        // when - 두 번째 페이지
        ResultActions resultActions2 = mvc.perform(get("/api/v1/chats")
                        .param("page", "1")
                        .param("size", "2"))
                .andDo(print());

        // then
        resultActions2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.first").value(false))
                .andExpect(jsonPath("$.page.last").value(true))
                .andExpect(jsonPath("$.page.hasNext").value(false));
    }
}