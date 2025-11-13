package com.back.domain.region.controller;

import com.back.domain.region.dto.RegionResBody;
import com.back.domain.region.service.RegionService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController implements RegionApi {

    private final RegionService regionService;

    @GetMapping
    public ResponseEntity<RsData<List<RegionResBody>>> readRegions() {
        List<RegionResBody> regionResBodyList = regionService.getRegions();
        RsData<List<RegionResBody>> response = new RsData<>(HttpStatus.OK, "지역 목록 조회 성공", regionResBodyList);
        return ResponseEntity.ok(response);
    }
}
