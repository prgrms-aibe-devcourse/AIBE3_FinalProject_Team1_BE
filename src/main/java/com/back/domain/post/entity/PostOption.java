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
public class PostOption extends BaseEntity {

    private String name;

    private Integer deposit;

    private Integer fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public PostOption(Post post, String name, Integer deposit, Integer fee) {
        this.name = name;
        this.deposit = deposit;
        this.fee = fee;
        this.setPost(post);
    }

    void setPost(Post post) {
        this.post = post;
    }

}
