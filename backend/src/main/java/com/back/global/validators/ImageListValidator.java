package com.back.global.validators;

import com.back.global.annotations.ValidateImages;
import com.back.global.exception.ServiceException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public class ImageListValidator implements ConstraintValidator<ValidateImages, List<MultipartFile>> {

    private long maxSize;
    private int maxCount;
    private Set<String> allowedTypes;

    @Override
    public void initialize(ValidateImages annotation) {
        this.maxSize = annotation.maxSize();
        this.maxCount = annotation.maxCount();
        this.allowedTypes = Set.of(annotation.allowedTypes());
    }

    @Override
    public boolean isValid(List<MultipartFile> images, ConstraintValidatorContext context) {
        validateList(images);
        validateEach(images);

        return true;
    }

    private void validateList(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지가 비어 있습니다.");
        }

        if (images.size() > maxCount) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, String.format("이미지는 최대 %d개까지 업로드할 수 있습니다.", maxCount));
        }

        long totalSize = images.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (totalSize > maxSize) {
            throw new ServiceException(
                    HttpStatus.BAD_REQUEST,
                    String.format("파일 크기는 %.0fMB를 초과할 수 없습니다. (현재: %.2fMB)",
                            maxSize / (1024.0 * 1024.0),
                            totalSize / (1024.0 * 1024.0))
            );
        }
    }

    private void validateEach(List<MultipartFile> images) {
        for (MultipartFile image : images) {

            if (image.isEmpty()) {
                throw new ServiceException(HttpStatus.BAD_REQUEST, "비어있는 이미지 파일이 포함되어 있습니다.");
            }

            String contentType = image.getContentType();

            if (!StringUtils.hasText(contentType) || !allowedTypes.contains(contentType.toLowerCase())) {
                throw new ServiceException(
                        HttpStatus.BAD_REQUEST,
                        "지원하지 않는 이미지 형식입니다. PNG, JPEG, WEBP, GIF 파일만 업로드 가능합니다."
                );
            }
        }
    }
}
