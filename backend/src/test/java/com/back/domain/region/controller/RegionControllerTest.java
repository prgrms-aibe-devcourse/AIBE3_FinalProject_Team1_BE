package com.back.domain.region.controller;

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
@Sql("/sql/regions.sql")
class RegionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("지역 목록 조회 성공 - 최상위 지역 2개")
    void readRegions_withRegions() throws Exception {

        mockMvc.perform(get("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("서울특별시"))
                .andExpect(jsonPath("$.data[1].name").value("경기도"));
    }

    @Test
    @DisplayName("계층 구조 포함 - 서울특별시 → 강남구, 서초구")
    void readRegions_withHierarchy() throws Exception {

        mockMvc.perform(get("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].child", hasSize(2)))
                .andExpect(jsonPath("$.data[0].child[0].name").value("강남구"))
                .andExpect(jsonPath("$.data[0].child[1].name").value("서초구"));
    }
}
