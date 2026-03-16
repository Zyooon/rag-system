package com.example.rag_project.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.rag_project.constants.ErrorConstants;

import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, WebRequest request) {
        // 에러 로그 기록
        logger.error("예외 발생: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorConstants.RESPONSE_KEY_SUCCESS, false);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_ERROR, ErrorConstants.ERROR_TYPE_INTERNAL_SERVER);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_MESSAGE, ex.getMessage());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_PATH, request.getDescription(false).replace("uri=", ""));
        errorResponse.put(ErrorConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException ex, WebRequest request) {
        // 에러 로그 기록
        logger.error("런타임 예외: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorConstants.RESPONSE_KEY_SUCCESS, false);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_ERROR, ErrorConstants.ERROR_TYPE_RUNTIME);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_MESSAGE, ex.getMessage());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_PATH, request.getDescription(false).replace("uri=", ""));
        errorResponse.put(ErrorConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RagServiceException.class)
    public ResponseEntity<Map<String, Object>> handleRagServiceExceptions(RagServiceException ex, WebRequest request) {
        // 에러 로그 기록
        logger.error("RAG 서비스 예외: {} - {}", ex.getClassName(), ex.getMessage());

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorConstants.RESPONSE_KEY_SUCCESS, false);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_ERROR, ErrorConstants.ERROR_TYPE_RAG_SERVICE);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_MESSAGE, ex.getMessage());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_PATH, request.getDescription(false).replace("uri=", ""));
        errorResponse.put(ErrorConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentExceptions(IllegalArgumentException ex, WebRequest request) {
        // 에러 로그 기록
        logger.warn("잘못된 인자: {}", ex.getMessage());

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorConstants.RESPONSE_KEY_SUCCESS, false);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_ERROR, ErrorConstants.ERROR_TYPE_INVALID_ARGUMENT);
        errorResponse.put(ErrorConstants.RESPONSE_KEY_MESSAGE, ex.getMessage());
        errorResponse.put(ErrorConstants.RESPONSE_KEY_PATH, request.getDescription(false).replace("uri=", ""));
        errorResponse.put(ErrorConstants.RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

}
