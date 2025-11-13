package com.back.domain.region.service;

import com.back.domain.region.dto.RegionCreateReqBody;
import com.back.domain.region.dto.RegionResBody;
import com.back.domain.region.dto.RegionUpdateReqBody;
import com.back.domain.region.entity.Region;
import com.back.domain.region.repository.RegionRepository;
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
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionResBody createRegion(RegionCreateReqBody regionCreateReqBody) {
        Long parentId = regionCreateReqBody.parentId();
        String regionName = regionCreateReqBody.name();
        if (parentId == null) {
            return createRegionWithoutParent(regionName);
        }

        return createRegionWithParent(parentId, regionName);
    }

    private RegionResBody createRegionWithParent(Long parentId, String regionName) {
        Region parentRegion = regionRepository.findById(parentId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 지역은 존재하지 않습니다.".formatted(parentId))
        );

        // Depth 검사: Depth 2까지만 허용
        // Depth 허용이 깊어지면 Depth 컬럼 추가하여 관리 필요
        if (parentRegion.getParent() != null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "지역은 Depth 2까지만 생성할 수 있습니다.");
        }

        Region region = Region.create(regionName, parentRegion);

        Region saved = regionRepository.save(region);
        return RegionResBody.of(saved);
    }

    private RegionResBody createRegionWithoutParent(String regionName) {
        Region region = Region.create(regionName, null);

        Region saved = regionRepository.save(region);
        return RegionResBody.of(saved);
    }

    public RegionResBody updateRegion(Long regionId, RegionUpdateReqBody regionUpdateReqBody) {
        Region region = regionRepository.findRegionWithChildById(regionId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 지역은 존재하지 않습니다.".formatted(regionId))
        );

        region.modify(regionUpdateReqBody);
        return RegionResBody.of(region);
    }

    public void deleteRegion(Long regionId) {
        try {
            regionRepository.deleteById(regionId);
            regionRepository.flush();
        } catch (DataIntegrityViolationException e) { // DB FK 제약 조건 위반 시 발생에러, 데이터 베이스에 FK 설정 필요 (PostRegion 테이블)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "%d번 지역을 참조 중인 게시글이 있습니다.".formatted(regionId));
        }
    }

    @Transactional(readOnly = true)
    public List<RegionResBody> getRegions() {
        List<Region> regionList = regionRepository.findAllWithChildren();
        return regionList.stream()
                .map(RegionResBody::of)
                .toList();
    }
}
