package com.back.domain.region.controller;

import com.back.domain.region.dto.RegionCreateReqBody;
import com.back.domain.region.dto.RegionResBody;
import com.back.domain.region.dto.RegionUpdateReqBody;
import com.back.domain.region.service.RegionService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/regions")
public class RegionAdmController implements RegionAdmApi {

    private final RegionService regionService;

    @PostMapping
    public ResponseEntity<RsData<RegionResBody>> createRegion(@Valid @RequestBody RegionCreateReqBody regionCreateReqBody) {
        RegionResBody regionResBody = regionService.createRegion(regionCreateReqBody);
        RsData<RegionResBody> response = new RsData<>(HttpStatus.CREATED, "지역 등록 성공", regionResBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RsData<RegionResBody>> updateRegion(
            @PathVariable("id") Long regionId,
            @Valid @RequestBody RegionUpdateReqBody regionUpdateReqBody) {
        RegionResBody regionResBody = regionService.updateRegion(regionId, regionUpdateReqBody);
        RsData<RegionResBody> response = new RsData<>(HttpStatus.OK, "지역 수정 성공", regionResBody);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RsData<Void>> deleteRegion(@PathVariable("id") Long regionId) {
        regionService.deleteRegion(regionId);
        RsData<Void> response = new RsData<>(HttpStatus.OK, "지역 삭제 성공");
        return ResponseEntity.ok(response);
    }
}
