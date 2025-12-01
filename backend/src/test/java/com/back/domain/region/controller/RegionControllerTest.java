package com.back.domain.region.controller;

import com.back.config.TestConfig;
import com.back.domain.region.common.ChildRegion;
import com.back.domain.region.dto.RegionResBody;
import com.back.domain.region.service.RegionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionService regionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("지역 목록 조회 성공 - 지역이 있는 경우")
    void readRegions_withRegions() throws Exception {
        // given
        RegionResBody region1 = new RegionResBody(1L, "서울", List.of());
        RegionResBody region2 = new RegionResBody(2L, "부산", List.of());
        List<RegionResBody> regions = Arrays.asList(region1, region2);

        when(regionService.getRegions()).thenReturn(regions);

        // when & then
        mockMvc.perform(get("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("서울"))
                .andExpect(jsonPath("$.data[0].child").isEmpty())
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("부산"))
                .andExpect(jsonPath("$.data[1].child").isEmpty());
    }

    @Test
    @DisplayName("지역 목록 조회 성공 - 빈 목록")
    void readRegions_withEmptyList() throws Exception {
        // given
        when(regionService.getRegions()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("지역 목록 조회 성공 - 계층 구조 포함")
    void readRegions_withHierarchy() throws Exception {
        // given
        RegionResBody childRegion = new RegionResBody(2L, "강남구", List.of());
        RegionResBody parentRegion = new RegionResBody(
                1L,
                "서울",
                Stream.of(childRegion).map(r -> new ChildRegion(r.id(), r.name())).toList()
        );
        List<RegionResBody> regions = List.of(parentRegion);

        when(regionService.getRegions()).thenReturn(regions);

        // when & then
        mockMvc.perform(get("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("서울"))
                .andExpect(jsonPath("$.data[0].child", hasSize(1)))
                .andExpect(jsonPath("$.data[0].child[0].id").value(2))
                .andExpect(jsonPath("$.data[0].child[0].name").value("강남구"));
    }
}