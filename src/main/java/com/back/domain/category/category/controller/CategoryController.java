package com.back.domain.category.category.controller;

import com.back.domain.category.category.dto.CategoryResBody;
import com.back.domain.category.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResBody>> readCategories() {
        List<CategoryResBody> categoryResBodyList = categoryService.getCategories();
        return ResponseEntity.ok(categoryResBodyList);
    }
}
