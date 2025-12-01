package com.back.domain.post.service;

import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.post.dto.res.GenPostDetailResBody;
import com.back.global.jpa.entity.BaseEntity;
import com.back.global.optimizer.ImageOptimizer;
import com.back.standard.util.json.JsonUt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostContentGenerateService {

    private final ChatClient chatClient;
    private final CategoryRepository categoryRepository;
    private final ImageOptimizer imageOptimizer;

    @Value("${custom.ai.post-detail-gen-prompt}")
    private String systemPrompt;

    @Value("${custom.ai.user.default-post-detail-gen-prompt}")
    private String defaultUserPrompt;

    public PostContentGenerateService(
            @Qualifier("gpt51ChatClient") ChatClient chatClient,
            CategoryRepository categoryRepository,
            ImageOptimizer imageOptimizer) {
        this.chatClient = chatClient;
        this.categoryRepository = categoryRepository;
        this.imageOptimizer = imageOptimizer;
    }

    public GenPostDetailResBody generatePostDetail(List<MultipartFile> imageFiles, String additionalInfo) {

        List<Resource> optimizedImages = imageOptimizer.optimizeImages(imageFiles);

        String categoriesJson = getCategoriesJson();

        String userPrompt = defaultUserPrompt
                .replace("{categoriesJson}", categoriesJson)
                .replace("{additionalInfo}", additionalInfo == null ? "" : additionalInfo);

        GenPostDetailResBody resBody = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> {
                    user.text(userPrompt);
                    for (Resource image : optimizedImages) {
                        user.media(MediaType.IMAGE_JPEG, image);
                    }
                })
                .call()
                .entity(GenPostDetailResBody.class);

        return resBody;
    }

    private String getCategoriesJson() {
        List<Category> categories = categoryRepository.findAllByParentIsNotNull();

        Map<Long, String> childCategories = categories.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Category::getName));

        return JsonUt.toString(childCategories  , "[]");
    }
}
