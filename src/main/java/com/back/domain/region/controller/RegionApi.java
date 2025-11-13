package com.back.domain.region.controller;

import com.back.domain.region.dto.RegionResBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Region API", description = "지역 조회 API, 인증없이 접근 가능")
public interface RegionApi {

    @Operation(summary = "지역 목록 조회 API", description = "지역들과 함께 연관된 하위 지역들 목록 조회")
    ResponseEntity<RsData<List<RegionResBody>>> readRegions();
}
