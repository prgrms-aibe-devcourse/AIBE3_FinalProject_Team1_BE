package com.back.domain.category.category.controller;

import com.back.domain.category.common.ChildCategory;
import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 카테고리가 있는 경우")
    void readCategories_withCategories() throws Exception {
        // given
        CategoryResBody category1 = new CategoryResBody(1L, "카테고리1", List.of());
        CategoryResBody category2 = new CategoryResBody(2L, "카테고리2", List.of());
        List<CategoryResBody> categories = Arrays.asList(category1, category2);

        when(categoryService.getCategories()).thenReturn(categories);

        // when & then
        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("카테고리1"))
                .andExpect(jsonPath("$.data[0].child").isEmpty())
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("카테고리2"))
                .andExpect(jsonPath("$.data[1].child").isEmpty());
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 빈 목록")
    void readCategories_withEmptyList() throws Exception {
        // given
        when(categoryService.getCategories()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 계층 구조 포함")
    void readCategories_withHierarchy() throws Exception {
        // given
        CategoryResBody childCategory = new CategoryResBody(2L, "하위카테고리", List.of());
        CategoryResBody parentCategory = new CategoryResBody(
                1L,
                "상위카테고리",
                List.of(new ChildCategory(childCategory.id(), childCategory.name()))
        );
        List<CategoryResBody> categories = List.of(parentCategory);

        when(categoryService.getCategories()).thenReturn(categories);

        // when & then
        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("상위카테고리"))
                .andExpect(jsonPath("$.data[0].child", hasSize(1)))
                .andExpect(jsonPath("$.data[0].child[0].id").value(2))
                .andExpect(jsonPath("$.data[0].child[0].name").value("하위카테고리"));
    }
}