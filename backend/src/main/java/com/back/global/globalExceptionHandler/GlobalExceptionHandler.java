package com.back.global.globalExceptionHandler;

import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, NoSuchElementException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return new ResponseEntity<>(
                new RsData<>(
                        NOT_FOUND,
                        "존재하지 않는 데이터입니다."
                ),
                NOT_FOUND
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, MethodArgumentTypeMismatchException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        String param = e.getName();
        String requiredType = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "unknown";

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "파라미터 %s 는  %s 타입이어야 합니다.".formatted(param, requiredType)
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, MethodArgumentNotValidException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

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
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, HttpMessageNotReadableException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "잘못된 요청입니다."
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, ServiceException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        RsData<Void> rsData = e.getRsData();

        return new ResponseEntity<>(
                rsData,
                ResponseEntity.status(rsData.status()).build().getStatusCode()
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, MissingRequestHeaderException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "회원정보를 찾을 수 없습니다."
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, ConversionFailedException e) {

        log.warn("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return new ResponseEntity<>(
                new RsData<>(
                        BAD_REQUEST,
                        "잘못된 요청입니다."
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handle(HttpServletRequest request, Exception e) {

        log.error("error at [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return new ResponseEntity<>(
                new RsData<>(
                        INTERNAL_SERVER_ERROR,
                        "서버 내부 오류입니다. 서버 로그 확인필요"
                ),
                INTERNAL_SERVER_ERROR
        );
    }
}
