package com.back.domain.category.controller;

import com.back.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
@Sql("/sql/categories.sql")
class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 최상위 카테고리 3개")
    void readCategories_withCategories() throws Exception {

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("스포츠"))
                .andExpect(jsonPath("$.data[1].name").value("가전"))
                .andExpect(jsonPath("$.data[2].name").value("디지털"));
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 계층 구조 포함")
    void readCategories_withHierarchy() throws Exception {

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("스포츠"))
                .andExpect(jsonPath("$.data[0].child", hasSize(3)))
                .andExpect(jsonPath("$.data[0].child[0].name").value("등산/아웃도어"))
                .andExpect(jsonPath("$.data[0].child[1].name").value("구기종목"))
                .andExpect(jsonPath("$.data[0].child[2].name").value("골프/라켓"))
                .andExpect(jsonPath("$.data[1].name").value("가전"))
                .andExpect(jsonPath("$.data[1].child", hasSize(2)))
                .andExpect(jsonPath("$.data[1].child[0].name").value("청소/세탁"))
                .andExpect(jsonPath("$.data[1].child[1].name").value("주방/생활가전"))
                .andExpect(jsonPath("$.data[2].name").value("디지털"))
                .andExpect(jsonPath("$.data[2].child", hasSize(3)))
                .andExpect(jsonPath("$.data[2].child[0].name").value("컴퓨터/노트북"))
                .andExpect(jsonPath("$.data[2].child[1].name").value("카메라/영상기기"))
                .andExpect(jsonPath("$.data[2].child[2].name").value("스마트폰/태블릿"));
    }
}
