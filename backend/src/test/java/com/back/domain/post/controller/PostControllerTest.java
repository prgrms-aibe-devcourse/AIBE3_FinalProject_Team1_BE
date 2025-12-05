package com.back.domain.post.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.back.config.TestConfig;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Sql(scripts = {
	"/sql/members.sql",
	"/sql/categories.sql",
	"/sql/regions.sql",
	"/sql/posts.sql",
	"/sql/post_images.sql",
	"/sql/post_regions.sql",
	"/sql/post_options.sql"
})
class PostControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("게시글 단건 조회 테스트")
	@WithUserDetails("user1@example.com")
	void getPostById_success() throws Exception {

		mockMvc.perform(get("/api/v1/posts/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").exists())
			.andExpect(jsonPath("$.data.id").value(1L))
			.andExpect(jsonPath("$.data.images").isArray())
			.andExpect(jsonPath("$.data.images.length()").value(1))
			.andExpect(jsonPath("$.data.options").isArray())
			.andExpect(jsonPath("$.data.options.length()").value(1))
			.andExpect(jsonPath("$.data.regionIds").isArray())
			.andExpect(jsonPath("$.data.regionIds.length()").value(1));
	}

	@Test
	@DisplayName("게시글 목록 조회 테스트")
	@WithUserDetails("user1@example.com")
	void getPostList_success() throws Exception {

		mockMvc.perform(get("/api/v1/posts")
				.param("page", "0")
				.param("size", "30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value("성공"))
			.andExpect(jsonPath("$.data.page.page").value(0))
			.andExpect(jsonPath("$.data.page.size").value(30))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(6));
	}

	@Test
	@DisplayName("내 게시글 조회 테스트")
	@WithUserDetails("user1@example.com")
	void getMyPostList_success() throws Exception {

		mockMvc.perform(get("/api/v1/posts/my")
				.param("page", "0")
				.param("size", "30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value("성공"))
			.andExpect(jsonPath("$.data.page.page").value(0))
			.andExpect(jsonPath("$.data.page.size").value(30))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3));

	}

	@Test
	@DisplayName("게시글 즐겨찾기 토글 테스트")
	@WithUserDetails("user1@example.com")
	void toggleFavorite_success() throws Exception {

		mockMvc.perform(post("/api/v1/posts/favorites/{id}", 4L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(true));

		mockMvc.perform(post("/api/v1/posts/favorites/{id}", 4L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(false));
	}

	@Test
	@DisplayName("게시글 즐겨찾기 조회 테스트")
	@WithUserDetails("user1@example.com")
	void getFavoritePosts_success() throws Exception {

		mockMvc.perform(post("/api/v1/posts/favorites/{id}", 4L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(true));

		mockMvc.perform(post("/api/v1/posts/favorites/{id}", 5L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(true));

		mockMvc.perform(post("/api/v1/posts/favorites/{id}", 6L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(true));

		mockMvc.perform(get("/api/v1/posts/favorites"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value("성공"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3));
	}

	@Test
	@DisplayName("게시글 삭제 테스트")
	@WithUserDetails("user1@example.com")
	void deletePost_success() throws Exception {

		mockMvc.perform(delete("/api/v1/posts/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value("게시글이 삭제되었습니다."));
	}

	@Test
	@DisplayName("다른 사용자의 게시글 삭제 시 실패 테스트")
	@WithUserDetails("user1@example.com")
	void deletePost_fail() throws Exception {

		mockMvc.perform(delete("/api/v1/posts/{id}", 4L))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.msg").value("본인의 게시글만 삭제할 수 있습니다."));
	}
}
