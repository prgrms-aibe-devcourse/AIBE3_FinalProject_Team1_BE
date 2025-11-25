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
    private long minSize;
    private Set<String> allowedTypes;
    private Set<String> allowedExtensions;

    @Override
    public void initialize(ValidateImages annotation) {
        this.maxSize = annotation.maxSize();
        this.minSize = annotation.minSize();
        this.allowedTypes = Set.of(annotation.allowedTypes());
        this.allowedExtensions = Set.of(annotation.allowedExtensions());
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext context) {
        if (files == null || files.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "업로드된 이미지가 없습니다.");
        }

        files.forEach(this::validateImageFile);
        return true;
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드 가능합니다.");
        }

        if (!allowedTypes.contains(contentType.toLowerCase())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. PNG, JPEG, WEBP, GIF 파일만 업로드 가능합니다.");
        }

        if (!StringUtils.hasText(originalFilename)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "파일명이 유효하지 않습니다.");
        }

        boolean isValidExtension = allowedExtensions.stream().anyMatch(originalFilename.toLowerCase()::endsWith);

        if (!isValidExtension) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. PNG, JPEG, WEBP, GIF 파일만 업로드 가능합니다.");
        }

        if (file.getSize() > maxSize) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    String.format("파일 크기는 %.0fMB를 초과할 수 없습니다. (현재: %.2fMB)",
                            maxSize / (1024.0 * 1024.0),
                            file.getSize() / (1024.0 * 1024.0))
            );
        }

        if (file.getSize() < minSize) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "파일이 너무 작습니다. 유효한 이미지를 업로드해주세요.");
        }
    }
}
