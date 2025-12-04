package com.back.domain.region.controller;

import com.back.config.TestConfig;
import com.back.domain.region.entity.Region;
import com.back.domain.region.repository.RegionRepository;
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
@Sql("/sql/regions.sql")
class RegionAdmControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegionRepository regionRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("지역 생성 - 성공")
    void createRegion_success() throws Exception {

        String json = """
                {
                  "parentId": 1,
                  "name": "새 지역"
                }
                """;

        mockMvc.perform(post("/api/v1/adm/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("지역 등록 성공"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("새 지역"))
                .andExpect(jsonPath("$.data.child").isEmpty());

        List<Region> all = regionRepository.findAll();
        assertTrue(all.stream().anyMatch(r -> r.getName().equals("새 지역")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("지역 수정 - 성공")
    void updateRegion_success() throws Exception {

        String json = """
                {
                  "name": "수정된 지역"
                }
                """;

        mockMvc.perform(patch("/api/v1/adm/regions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("지역 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("수정된 지역"))
                .andExpect(jsonPath("$.data.child", hasSize(2)));


        Region updated = regionRepository.findById(2L).orElseThrow();
        assertEquals("수정된 지역", updated.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("지역 삭제 - 성공")
    void deleteRegion_success() throws Exception {

        mockMvc.perform(delete("/api/v1/adm/regions/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("지역 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertFalse(regionRepository.existsById(3L));
    }
}