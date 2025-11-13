package com.back.domain.category.controller;

import com.back.domain.category.dto.CategoryCreateReqBody;
import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.dto.CategoryUpdateReqBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Category Admin API", description = "카테고리 관리자 API, 관리자 인증 필요")
public interface CategoryAdmApi {

    @Operation(summary = "카테고리 등록 API", description = "parentId = null : 상위 카테고리 등록, parentId가 있다면 해당 Id의 하위 카테고리로 등록")
    ResponseEntity<RsData<CategoryResBody>> createCategory(@Valid @RequestBody CategoryCreateReqBody categoryCreateReqBody);

    @Operation(summary = "카테고리 수정 API", description = "카테고리 이름 수정, 수정된 카테고리와 함께 연관된 하위 카테고리들 응답")
    ResponseEntity<RsData<CategoryResBody>> updateCategory(
            @PathVariable("id") Long categoryId,
            @Valid @RequestBody CategoryUpdateReqBody categoryUpdateReqBody);

    @Operation(summary = "카테고리 삭제 API", description = "해당 카테고리와 함께 연관된 하위 카테고리들도 삭제")
    ResponseEntity<RsData<Void>> deleteCategory(@PathVariable("id") Long categoryId);
}
