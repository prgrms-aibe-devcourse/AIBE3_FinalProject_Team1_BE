package com.back.domain.post.post.controller;

import com.back.domain.member.member.dto.AuthorDto;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.dto.req.PostCreateReqBody;
import com.back.domain.post.post.dto.req.PostImageReqBody;
import com.back.domain.post.post.dto.req.PostOptionReqBody;
import com.back.domain.post.post.dto.res.PostDetailResBody;
import com.back.domain.post.post.dto.res.PostImageResBody;
import com.back.domain.post.post.dto.res.PostListResBody;
import com.back.domain.post.post.dto.res.PostOptionResBody;
import com.back.domain.post.post.service.PostService;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PageUt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    private SecurityUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new SecurityUser(
                1L,
                "test@test.com",
                "12345678",
                "jjuchan",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        BDDMockito.given(postService.createPost(any(), anyLong()))
                .willReturn(1L);
    }

    @Test
    @DisplayName("게시글 등록")
    void createPost_success() throws Exception {
        // given
        PostCreateReqBody reqBody = PostCreateReqBody.builder()
                .title("맥북 대여합니다.")
                .content("맥북프로 16인치 풀옵션입니다.")
                .receiveMethod(ReceiveMethod.DIRECT)
                .returnMethod(ReturnMethod.DIRECT)
                .returnAddress1("서울특별시 강남구 테헤란로 1")
                .returnAddress2("101호")
                .regionIds(List.of(1L))
                .categoryId(1L)
                .deposit(10000)
                .fee(3000)
                .options(List.of(
                        new PostOptionReqBody("마우스 포함", 5000, 0)
                ))
                .images(List.of(new PostImageReqBody(true)))
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                        .with(SecurityMockMvcRequestPostProcessors.user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("게시글이 등록되었습니다."))
                .andExpect(jsonPath("$.postId").value(1));
    }

    @Test
    @DisplayName("게시글 목록 조회 - 페이징 처리 성공")
    void getPostList_paging_success() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostListResBody> mockPage = new PageImpl<>(
                List.of(
                        PostListResBody.builder()
                                .postId(1L)
                                .title("맥북 대여합니다.")
                                .thumbnailImageUrl("https://example.com/thumb.jpg")
                                .authorNickname("jjuchan")
                                .fee(3000)
                                .deposit(10000)
                                .build()
                ),
                pageable,
                1
        );
        // when & then
        BDDMockito.given(postService.getPostList(any()))
                .willReturn(PageUt.of(mockPage));

        mockMvc.perform(get("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("맥북 대여합니다."));
    }

    @Test
    @DisplayName("게시글 상세조회")
    void getPostDetail_success() throws Exception {
        //given
        Long postId = 1L;

        PostDetailResBody MockDetail = PostDetailResBody.builder()
                .postId(postId)
                .title("맥북 대여합니다.")
                .content("맥북프로 16인치 풀옵션입니다.")
                .categoryId(1L)
                .regionIds(List.of(1L))
                .receiveMethod(ReceiveMethod.DIRECT)
                .returnMethod(ReturnMethod.DIRECT)
                .returnAddress1("서울특별시 강남구 테헤란로 1")
                .returnAddress2("101호")
                .deposit(10000)
                .fee(3000)
                .options(List.of(
                        PostOptionResBody.builder()
                                .name("마우스 포함")
                                .deposit(5000)
                                .fee(0)
                                .build()
                ))
                .images(List.of(
                        PostImageResBody.builder()
                                .file("https://example.com/image1.jpg")
                                .isPrimary(true)
                                .build()
                ))
                .createdAt(null)
                .modifiedAt(null)
                .author(AuthorDto.builder()
                        .id(10L)
                        .nickname("jjuchan")
                        .profileImgUrl("https://example.com/profile.jpg")
                        .build())
                .isFavorite(true)
                .isBanned(false)
                .build();

        BDDMockito.given(postService.getPostById(postId))
                .willReturn(MockDetail);

        //when & then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value("맥북 대여합니다."))
                .andExpect(jsonPath("$.author.nickname").value("jjuchan"))
                .andExpect(jsonPath("$.images[0].isPrimary").value(true))
                .andExpect(jsonPath("$.options[0].name").value("마우스 포함"));
    }

    @Test
    @DisplayName("내 게시글 조회")
    void getMyPosts_success() throws Exception {
        // given
        List<PostListResBody> mockMyPosts = List.of(
                PostListResBody.builder()
                        .postId(1L)
                        .title("맥북 대여합니다.")
                        .thumbnailImageUrl("https://example.com/thumb1.jpg")
                        .categoryId(2L)
                        .regionIds(List.of(1L, 2L))
                        .receiveMethod(ReceiveMethod.DELIVERY)
                        .returnMethod(ReturnMethod.DELIVERY)
                        .fee(5000)
                        .deposit(10000)
                        .authorNickname("jjuchan")
                        .isFavorite(false)
                        .isBanned(false)
                        .build(),
                PostListResBody.builder()
                        .postId(2L)
                        .title("아이패드 빌려드려요")
                        .thumbnailImageUrl("https://example.com/thumb2.jpg")
                        .categoryId(3L)
                        .regionIds(List.of(3L))
                        .receiveMethod(ReceiveMethod.DELIVERY)
                        .returnMethod(ReturnMethod.DELIVERY)
                        .fee(3000)
                        .deposit(5000)
                        .authorNickname("jjuchan")
                        .isFavorite(true)
                        .isBanned(false)
                        .build()
        );

        BDDMockito.given(postService.getMyPosts(anyLong()))
                .willReturn(mockMyPosts);

        // when & then
        mockMvc.perform(get("/api/v1/posts/my")
                        .with(SecurityMockMvcRequestPostProcessors.user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("맥북 대여합니다."))
                .andExpect(jsonPath("$[1].title").value("아이패드 빌려드려요"))
                .andExpect(jsonPath("$[0].authorNickname").value("jjuchan"))
                .andExpect(jsonPath("$[0].receiveMethod").value("DELIVERY"));
    }

}

