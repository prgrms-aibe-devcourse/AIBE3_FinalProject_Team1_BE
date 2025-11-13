package com.back.domain.region.repository;

import com.back.domain.region.entity.Region;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    @EntityGraph(attributePaths = {"children"})
    Optional<Region> findRegionWithChildById(Long regionId);

    @EntityGraph(attributePaths = {"children"})
    @Query("SELECT r FROM Region r WHERE r.parent IS NULL")
    List<Region> findAllWithChildren();
}
