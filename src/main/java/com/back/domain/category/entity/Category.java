package com.back.domain.category.entity;

import com.back.domain.category.dto.CategoryUpdateReqBody;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children = new ArrayList<>();

    private Category(String name, Category parent) {
        this.name = name;
        this.parent = parent;
    }

    public static Category create(String name, Category parent) {
        return new Category(name, parent);
    }

    public void modify(CategoryUpdateReqBody categoryUpdateReqBody) {
        this.name = categoryUpdateReqBody.name();
    }
}
