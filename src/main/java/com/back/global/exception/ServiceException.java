package com.back.global.exception;

import com.back.global.rsData.RsData;

public class ServiceException extends RuntimeException {
    private final String resultCode;
    private final String message;

    public ServiceException(String resultCode, String message) {
        super(resultCode + " : " + message);
        this.resultCode = resultCode;
        this.message = message;
    }

    public RsData<Void> getRsData() { return new RsData<>(resultCode, message);}
}
