package com.back.domain.region.dto;

import com.back.domain.region.common.ChildRegion;
import com.back.domain.region.entity.Region;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record RegionResBody(
        Long id,
        String name,
        List<ChildRegion> child
) {
    public static RegionResBody of(Region region) {
        List<ChildRegion> children = Optional.ofNullable(region.getChildren())
                .orElse(List.of())
                .stream()
                .map(r -> new ChildRegion(r.getId(), r.getName()))
                .collect(Collectors.toList());

        return new RegionResBody(region.getId(), region.getName(), children);
    }
}
