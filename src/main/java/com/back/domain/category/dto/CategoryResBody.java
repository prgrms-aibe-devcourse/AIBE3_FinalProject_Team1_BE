package com.back.domain.category.dto;

import com.back.domain.category.common.ChildCategory;
import com.back.domain.category.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record CategoryResBody(
        Long id,
        String name,
        List<ChildCategory> child
) {
    public static CategoryResBody of(Category category) {
        List<ChildCategory> children = Optional.ofNullable(category.getChildren())
                .orElse(List.of())
                .stream()
                .map(c -> new ChildCategory(c.getId(), c.getName()))
                .collect(Collectors.toList());

        return new CategoryResBody(category.getId(), category.getName(), children);
    }
}
