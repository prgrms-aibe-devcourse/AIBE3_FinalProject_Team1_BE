package com.back.global.exception;

import com.back.global.rsData.RsData;
import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String message;

    public ServiceException(HttpStatus status, String message) {
        super(status + " : " + message);
        this.status = status;
        this.message = message;
    }

    public RsData<Void> getRsData() { return new RsData<>(status, message);}
}
