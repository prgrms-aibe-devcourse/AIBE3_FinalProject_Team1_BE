package com.back.global.rsData;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

public record RsData<T>(
        @NonNull int status,
        @NonNull String msg,
        @NonNull T data) {
    public RsData(HttpStatus status, String msg) {
        this(status, msg, null);
    }
    public RsData(HttpStatus status, String msg, T data) {
        this(status.value(), msg, data);
    }
}
