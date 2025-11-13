package com.back.domain.post.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostImage extends BaseEntity {

    private String imageUrl;

    private Boolean isPrimary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public PostImage(Post post, String imageUrl, Boolean isPrimary) {
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
        this.setPost(post);
    }

    void setPost(Post post) {
        this.post = post;
    }
}
