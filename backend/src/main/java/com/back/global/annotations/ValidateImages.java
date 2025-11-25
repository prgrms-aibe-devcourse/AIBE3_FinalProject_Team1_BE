package com.back.global.annotations;

import com.back.global.validators.ImageListValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageListValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateImages {

    String message() default "유효하지 않은 이미지 파일입니다.";

    long maxSize() default 50 * 1024 * 1024; // 50MB
    long minSize() default 100; // 100 bytes
    String[] allowedTypes() default {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
    String[] allowedExtensions() default {".jpeg", ".jpg", ".png", ".gif", ".webp"};

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
