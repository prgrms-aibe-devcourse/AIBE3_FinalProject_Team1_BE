
package com.back.domain.category.controller;

import com.back.config.TestConfig;
import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
@Sql("/sql/categories.sql")
class CategoryAdmControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CategoryRepository categoryRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("카테고리 생성 - 성공")
    void createCategory_success() throws Exception {

        String json = """
                {
                  "parentId": 1,
                  "name": "새 카테고리"
                }
                """;

        mockMvc.perform(post("/api/v1/adm/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value("카테고리 등록 성공"))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.name").value("새 카테고리"))
                .andExpect(jsonPath("$.data.child").isEmpty());

        List<Category> all = categoryRepository.findAll();
        assertTrue(all.stream().anyMatch(r -> r.getName().equals("새 카테고리")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("카테고리 수정 - 성공")
    void updateCategory_success() throws Exception {

        String json = """
                {
                  "name": "수정된 카테고리"
                }
                """;

        mockMvc.perform(patch("/api/v1/adm/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("수정된 카테고리"))
                .andExpect(jsonPath("$.data.child", hasSize(3)));

        Category updated = categoryRepository.findById(1L).orElseThrow();
        assertEquals("수정된 카테고리", updated.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("카테고리 삭제 - 성공")
    void deleteCategory_success() throws Exception {

        mockMvc.perform(delete("/api/v1/adm/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertFalse(categoryRepository.existsById(1L));
    }
}