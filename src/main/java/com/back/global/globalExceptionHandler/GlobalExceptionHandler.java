package com.back.global.globalExceptionHandler;

import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
@ResponseStatus(NOT_FOUND)
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "존재하지 않는 데이터입니다."
                ),
                NOT_FOUND
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted(Comparator.comparing(String::toString))
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        message
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException e) {

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "잘못된 요청입니다."
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<RsData<Void>> handle(ServiceException e) {
        RsData<Void> rsData = e.getRsData();

        return new ResponseEntity<>(
                rsData,
                ResponseEntity.status(rsData.status()).build().getStatusCode()
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<RsData<Void>> handle(MissingRequestHeaderException e) {

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "회원정보를 찾을 수 없습니다."
                ),
                BAD_REQUEST
        );
    }
}
