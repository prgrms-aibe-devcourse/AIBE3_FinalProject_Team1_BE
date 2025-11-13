package com.back.domain.category.controller;

import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<RsData<List<CategoryResBody>>> readCategories() {
        List<CategoryResBody> categoryResBodyList = categoryService.getCategories();
        RsData<List<CategoryResBody>> response = new RsData<>(HttpStatus.OK, "카테고리 목록 조회 성공", categoryResBodyList);
        return ResponseEntity.ok(response);
    }
}
