package com.back.domain.category.controller;

import com.back.domain.category.dto.CategoryResBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Category API", description = "카테고리 조회 API, 인증없이 접근 가능")
public interface CategoryApi {

    @Operation(summary = "카테고리 목록 조회 API", description = "카테고리들과 함께 연관된 하위 카테고리들 목록 조회")
    ResponseEntity<RsData<List<CategoryResBody>>> readCategories();
}
