package com.back.domain.region.entity;

import com.back.domain.region.dto.RegionUpdateReqBody;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Region> children = new ArrayList<>();

    private Region(String name, Region parent) {
        this.name = name;
        this.parent = parent;
    }

    public static Region create(String name, Region parent) {
        return new Region(name, parent);
    }

    public void modify(RegionUpdateReqBody regionUpdateReqBody) {
        this.name = regionUpdateReqBody.name();
    }
}
