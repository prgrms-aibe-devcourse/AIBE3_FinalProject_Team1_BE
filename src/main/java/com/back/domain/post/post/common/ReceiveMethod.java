package com.back.domain.post.post.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReceiveMethod {
    DIRECT("직거래"),
    DELIVERY("택배"),
    ANY("상관없음");

    private final String description;


}
