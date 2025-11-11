package com.back.domain.post.post.controller;

import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.dto.PostCreateReqBody;
import com.back.domain.post.post.dto.PostImageReqBody;
import com.back.domain.post.post.dto.PostOptionReqBody;
import com.back.domain.post.post.service.PostService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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
                .willReturn(RsData.success("게시글이 등록되었습니다.", 1L));
    }

    @Test
    @DisplayName("게시글 등록 성공 (Mock Service)")
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
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("게시글이 등록되었습니다."));
    }
}
