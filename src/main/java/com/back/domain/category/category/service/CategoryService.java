package com.back.domain.category.category.service;

import com.back.domain.category.category.dto.CategoryReqBody;
import com.back.domain.category.category.dto.CategoryResBody;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResBody createCategory(CategoryReqBody categoryReqBody) {
        Long parentId = categoryReqBody.parentId();
        String categoryName = categoryReqBody.name();
        if (parentId == null) {
            return createCategoryWithoutParent(categoryName);
        }

        return createCategoryWithParent(parentId, categoryName);
    }

    private CategoryResBody createCategoryWithParent(Long parentId, String categoryName) {
        Category parentCategory = categoryRepository.findById(parentId).orElseThrow(
                () -> new ServiceException("404-1", "parentId에 해당하는 카테고리가 없습니다.")
        );

        // Depth 검사: Depth 2까지만 허용
        // Depth 허용이 깊어지면 Depth 컬럼 추가하여 관리 필요
        if (parentCategory.getParent() != null) {
            throw new ServiceException("400-1", "카테고리는 Depth 2까지만 생성할 수 있습니다.");
        }

        Category category = Category.builder()
                .parent(parentCategory)
                .name(categoryName)
                .build();

        Category saved = categoryRepository.save(category);
        return new CategoryResBody(saved.getName(), null);
    }

    private CategoryResBody createCategoryWithoutParent(String categoryName) {
        Category category = Category.builder()
                .name(categoryName)
                .build();

        Category saved = categoryRepository.save(category);
        return new CategoryResBody(saved.getName(), null);
    }
}
