package com.back.domain.post.post.repository;

import com.back.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByAuthorId(Long memberId, Pageable pageable);

    @Query("""
                SELECT p FROM Post p
                WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:categoryId IS NULL OR p.category.id = :categoryId)
                AND (
                    :regionIds IS NULL 
                    OR EXISTS (
                        SELECT pr FROM PostRegion pr
                        WHERE pr.post = p AND pr.region.id IN :regionIds
                    )
                )
            """)
    Page<Post> findFilteredPosts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("regionIds") List<Long> regionIds,
            Pageable pageable
    );
}
