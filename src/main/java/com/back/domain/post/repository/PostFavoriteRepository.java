package com.back.domain.post.repository;

import com.back.domain.post.entity.PostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    Optional<PostFavorite> findByMemberIdAndPostId(Long postId, long memberId);

    Page<PostFavorite> findAllByMemberId(long memberId, Pageable pageable);
}
