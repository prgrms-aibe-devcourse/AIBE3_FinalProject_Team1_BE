package com.back.global.optimizer;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageOptimizer {

    private static final int MAX_DIMENSION = 1024;
    private static final double QUALITY = 0.85;
    private static final int MAX_IMAGE_COUNT = 10;

    public List<Resource> optimizeImages(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }

        return imageFiles.stream()
                .limit(MAX_IMAGE_COUNT)
                .map(this::optimizeToResource)
                .collect(Collectors.toList());
    }

    private Resource optimizeToResource(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return file.getResource();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(file.getInputStream())
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .outputFormat("jpg")
                    .outputQuality(QUALITY)
                    .toOutputStream(baos);

            byte[] optimizedBytes = baos.toByteArray();

            if (optimizedBytes.length >= file.getSize()) {
                return file.getResource();
            }

            return new ByteArrayResource(optimizedBytes);

        } catch (IOException e) {
            return file.getResource();
        }
    }
}