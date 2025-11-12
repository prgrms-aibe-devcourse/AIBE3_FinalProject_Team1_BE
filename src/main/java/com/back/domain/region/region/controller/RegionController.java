package com.back.domain.region.region.controller;

import com.back.domain.region.region.dto.RegionResBody;
import com.back.domain.region.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public ResponseEntity<List<RegionResBody>> readRegions() {
        List<RegionResBody> regionResBodyList = regionService.getRegions();
        return ResponseEntity.ok(regionResBodyList);
    }
}
