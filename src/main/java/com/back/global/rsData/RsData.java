package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

public record RsData<T>(
        @JsonIgnore @NonNull String resultCode,
        @NonNull int status,
        @NonNull String msg,
        @NonNull T data) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }
    public RsData(String resultCode, String msg, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-",2)[0]), msg, data);
    }

    public static <T> RsData<T> success(String msg, T data) {
        return new RsData<>("200-1", msg, data);
    }
    public static <T> RsData<T> success(String msg) {
        return new RsData<>("200-1", msg, null);
    }
}
