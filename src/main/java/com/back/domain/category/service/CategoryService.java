package com.back.domain.category.service;

import com.back.domain.category.dto.CategoryCreateReqBody;
import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.dto.CategoryUpdateReqBody;
import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResBody createCategory(CategoryCreateReqBody categoryCreateReqBody) {
        Long parentId = categoryCreateReqBody.parentId();
        String categoryName = categoryCreateReqBody.name();
        if (parentId == null) {
            return createCategoryWithoutParent(categoryName);
        }

        return createCategoryWithParent(parentId, categoryName);
    }

    private CategoryResBody createCategoryWithParent(Long parentId, String categoryName) {
        Category parentCategory = categoryRepository.findById(parentId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 카테고리는 존재하지 않습니다.".formatted(parentId))
        );

        // Depth 검사: Depth 2까지만 허용
        // Depth 허용이 깊어지면 Depth 컬럼 추가하여 관리 필요
        if (parentCategory.getParent() != null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "카테고리는 Depth 2까지만 생성할 수 있습니다.");
        }

        Category category = Category.create(categoryName, parentCategory);

        Category saved = categoryRepository.save(category);
        return CategoryResBody.of(saved);
    }

    private CategoryResBody createCategoryWithoutParent(String categoryName) {
        Category category = Category.create(categoryName, null);

        Category saved = categoryRepository.save(category);
        return CategoryResBody.of(saved);
    }

    public CategoryResBody updateCategory(Long categoryId, CategoryUpdateReqBody categoryUpdateReqBody) {
        Category category = categoryRepository.findCategoryWithChildById(categoryId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 카테고리는 존재하지 않습니다.".formatted(categoryId))
        );

        category.modify(categoryUpdateReqBody);
        return CategoryResBody.of(category);
    }

    public void deleteCategory(Long categoryId) {
        try {
            categoryRepository.deleteById(categoryId);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) { // DB FK 제약 조건 위반 시 발생에러, 데이터 베이스에 FK 설정 필요 (Post 테이블)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "%d번 카테고리를 참조 중인 게시글이 있습니다.".formatted(categoryId));
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResBody> getCategories() {
        List<Category> categoryList = categoryRepository.findAllWithChildren();
        return categoryList.stream()
                .map(CategoryResBody::of)
                .toList();
    }
}
