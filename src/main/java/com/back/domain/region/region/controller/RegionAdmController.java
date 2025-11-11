package com.back.domain.region.region.controller;

import com.back.domain.region.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/regions")
public class RegionAdmController {

    private final RegionService regionService;
}
