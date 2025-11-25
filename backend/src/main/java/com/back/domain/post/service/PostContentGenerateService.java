package com.back.domain.post.service;

import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.post.dto.res.GenPostDetailResBody;
import com.back.global.validator.OpenAiImageInputValidator;
import com.back.standard.util.json.JsonUt;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostContentGenerateService {

    private final ChatClient chatClient;
    private final CategoryRepository categoryRepository;

    @Value("${custom.ai.post-detail-gen-prompt}")
    private String systemPrompt;

    @Value("${custom.ai.user.default-post-detail-gen-prompt}")
    private String defaultUserPrompt;

    public GenPostDetailResBody generatePostDetail(List<MultipartFile> imageFiles) {
        OpenAiImageInputValidator.validateImages(imageFiles);

        String categoriesJson = getCategoriesJson();

        String userPrompt = defaultUserPrompt.replace("{categoriesJson}", categoriesJson);

        GenPostDetailResBody resBody = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> {
                    user.text(userPrompt);
                    for (MultipartFile file : imageFiles) {
                        user.media(MimeTypeUtils.parseMimeType(file.getContentType()), file.getResource());
                    }
                })
                .call()
                .entity(GenPostDetailResBody.class);

        return resBody;
    }

    private String getCategoriesJson() {
        List<Category> categories = categoryRepository.findAllWithChildren();

        List<CategoryResBody> categoryResBodies = categories.stream()
                .map(CategoryResBody::of)
                .collect(Collectors.toList());

        return JsonUt.toString(categoryResBodies, "[]");
    }
}
