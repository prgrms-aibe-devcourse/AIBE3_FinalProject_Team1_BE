package com.back.domain.chat.chat.controller;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.chat.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.chat.chat.service.ChatService;
import com.back.domain.member.common.MemberRole;
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
        member1 = memberRepository.save(Member.builder()
                .email("user1@test.com")
                .password("1234")
                .name("홍길동")
                .phoneNumber("010-1111-1111")
                .address1("서울시 강남구")
                .address2("테헤란로 123")
                .nickname("hong")
                .isBanned(false)
                .role(MemberRole.USER)
                .profileImgUrl(null)
                .build()
        );

        member2 = memberRepository.save(Member.builder()
                .email("user2@test.com")
                .password("1234")
                .name("김철수")
                .phoneNumber("010-2222-2222")
                .address1("서울시 서초구")
                .address2("서초대로 456")
                .nickname("kim")
                .isBanned(false)
                .role(MemberRole.USER)
                .profileImgUrl(null)
                .build()
        );

        member3 = memberRepository.save(Member.builder()
                .email("user3@test.com")
                .password("1234")
                .name("이영희")
                .phoneNumber("010-3333-3333")
                .address1("서울시 마포구")
                .address2("월드컵북로 789")
                .nickname("lee")
                .isBanned(false)
                .role(MemberRole.USER)
                .profileImgUrl(null)
                .build()
        );

        member4 = memberRepository.save(Member.builder()
                .email("user4@test.com")
                .password("1234")
                .name("박민수")
                .phoneNumber("010-4444-4444")
                .address1("서울시 송파구")
                .address2("잠실로 101")
                .nickname("park")
                .isBanned(false)
                .role(MemberRole.USER)
                .profileImgUrl(null)
                .build()
        );

        // 게시글 생성
        Category category = categoryRepository.save(
                Category.create("노트북", null)
        );

        post1 = postRepository.save(Post.of(
                "캠핑 텐트 대여",
                "4인용 텐트입니다.",
                ReceiveMethod.DELIVERY,
                ReturnMethod.DELIVERY,
                null,
                null,
                10000,
                5000,
                member2,
                category
        ));

        post2 = postRepository.save(Post.of(
                "노트북 대여합니다",
                "맥북 프로입니다.",
                ReceiveMethod.DIRECT,
                ReturnMethod.DIRECT,
                null,
                null,
                50000,
                20000,
                member3,
                category
        ));

        post3 = postRepository.save(Post.of(
                "카메라 렌탈",
                "소니 A7III입니다.",
                ReceiveMethod.ANY,
                ReturnMethod.ANY,
                null,
                null,
                30000,
                15000,
                member4,
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