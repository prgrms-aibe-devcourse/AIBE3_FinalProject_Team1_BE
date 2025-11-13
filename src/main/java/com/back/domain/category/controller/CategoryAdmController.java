package com.back.domain.category.controller;

import com.back.domain.category.dto.CategoryCreateReqBody;
import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.dto.CategoryUpdateReqBody;
import com.back.domain.category.service.CategoryService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/categories")
public class CategoryAdmController implements CategoryAdmApi {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<RsData<CategoryResBody>> createCategory(@Valid @RequestBody CategoryCreateReqBody categoryCreateReqBody) {
        CategoryResBody categoryResBody = categoryService.createCategory(categoryCreateReqBody);
        RsData<CategoryResBody> response = new RsData<>(HttpStatus.CREATED, "카테고리 등록 성공", categoryResBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RsData<CategoryResBody>> updateCategory(
            @PathVariable("id") Long categoryId,
            @Valid @RequestBody CategoryUpdateReqBody categoryUpdateReqBody) {
        CategoryResBody categoryResBody = categoryService.updateCategory(categoryId, categoryUpdateReqBody);
        RsData<CategoryResBody> response = new RsData<>(HttpStatus.OK, "카테고리 수정 성공", categoryResBody);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RsData<Void>> deleteCategory(@PathVariable("id") Long categoryId) {
        categoryService.deleteCategory(categoryId);
        RsData<Void> response = new RsData<>(HttpStatus.OK, "카테고리 삭제 성공");
        return ResponseEntity.ok(response);
    }
}
