package com.back.domain.category.repository;

import com.back.domain.category.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"children"})
    Optional<Category> findCategoryWithChildById(Long id);

    @EntityGraph(attributePaths = {"children"})
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findAllWithChildren();

    List<Category> findAllByParentIsNotNull();
}
