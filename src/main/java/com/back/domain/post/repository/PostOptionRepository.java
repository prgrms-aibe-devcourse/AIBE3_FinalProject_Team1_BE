package com.back.domain.post.repository;

import com.back.domain.post.entity.PostOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostOptionRepository extends JpaRepository<PostOption, Long> {
}
