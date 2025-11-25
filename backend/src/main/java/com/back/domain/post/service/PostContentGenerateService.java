package com.back.domain.post.service;

import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class PostContentGenerateService {

    private final ChatClient chatClient;

    @Value("${custom.ai.post-detail-gen-prompt}")
    private String systemPrompt;

    public String generatePostDetail(MultipartFile imageFile) {
        validateImageFile(imageFile);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userSpec -> userSpec
                        .text("이 사진을 기반으로 물품 대여 게시글을 작성해야하는데 해주라") // USER PROMPT 는 대체
                        .media(MimeType.valueOf(
                                Objects.requireNonNull(
                                        imageFile.getContentType())), imageFile.getResource())
                )
                .call()
                .content();

        return response;
    }

    // 이미지 파일 유효성 검사 (OpenAi 공식 문서 지원 기준)
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드 가능합니다.");
        }

        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

        boolean isValidType = false;
        for (String type : allowedTypes) {
            if (type.equalsIgnoreCase(contentType)) {
                isValidType = true;
                break;
            }
        }

        boolean isValidExtension = false;
        if (originalFilename != null) {
            String lowerFilename = originalFilename.toLowerCase();
            for (String ext : allowedExtensions) {
                if (lowerFilename.endsWith(ext)) {
                    isValidExtension = true;
                    break;
                }
            }
        }

        if (!isValidType || !isValidExtension) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. PNG, JPEG, WEBP, GIF 파일만 업로드 가능합니다.");
        }

        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new ServiceException(
                    HttpStatus.BAD_REQUEST,
                    String.format("파일 크기는 50MB를 초과할 수 없습니다. (현재: %.2fMB)",
                            file.getSize() / (1024.0 * 1024.0))
            );
        }

        long minSize = 100; // 100 bytes
        if (file.getSize() < minSize) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "파일이 너무 작습니다. 유효한 이미지를 업로드해주세요.");
        }
    }
}
