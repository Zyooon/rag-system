package com.example.rag_project.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, WebRequest request) {
        // 스택 트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 에러 발생 위치 정보 추출
        String errorLocation = extractErrorLocation(ex, stackTrace);

        // 에러 로그 기록
        logger.error("=== 글로벌 예외 처리 ===");
        logger.error("에러 발생 시간: {}", LocalDateTime.now());
        logger.error("에러 위치: {}", errorLocation);
        logger.error("에러 메시지: {}", ex.getMessage());
        logger.error("요청 URI: {}", request.getDescription(false));
        logger.error("스택 트레이스: {}", stackTrace);
        logger.error("========================");

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("location", errorLocation);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException ex, WebRequest request) {
        // 스택 트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 에러 발생 위치 정보 추출
        String errorLocation = extractErrorLocation(ex, stackTrace);

        // 에러 로그 기록
        logger.error("=== 런타임 예외 처리 ===");
        logger.error("에러 발생 시간: {}", LocalDateTime.now());
        logger.error("에러 위치: {}", errorLocation);
        logger.error("에러 메시지: {}", ex.getMessage());
        logger.error("요청 URI: {}", request.getDescription(false));
        logger.error("스택 트레이스: {}", stackTrace);
        logger.error("========================");

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "Runtime Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("location", errorLocation);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RagServiceException.class)
    public ResponseEntity<Map<String, Object>> handleRagServiceExceptions(RagServiceException ex, WebRequest request) {
        // 스택 트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 커스텀 예외의 위치 정보 사용
        String errorLocation = String.format("%s.%s:%d", ex.getClassName(), ex.getMethodName(), ex.getLineNumber());

        // 에러 로그 기록
        logger.error("=== RAG 서비스 예외 처리 ===");
        logger.error("에러 발생 시간: {}", LocalDateTime.now());
        logger.error("에러 위치: {}", errorLocation);
        logger.error("에러 메시지: {}", ex.getMessage());
        logger.error("요청 URI: {}", request.getDescription(false));
        logger.error("스택 트레이스: {}", stackTrace);
        logger.error("========================");

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "RAG Service Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("location", errorLocation);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentExceptions(IllegalArgumentException ex, WebRequest request) {
        // 스택 트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 에러 발생 위치 정보 추출
        String errorLocation = extractErrorLocation(ex, stackTrace);

        // 에러 로그 기록
        logger.error("=== 잘못된 인자 예외 처리 ===");
        logger.error("에러 발생 시간: {}", LocalDateTime.now());
        logger.error("에러 위치: {}", errorLocation);
        logger.error("에러 메시지: {}", ex.getMessage());
        logger.error("요청 URI: {}", request.getDescription(false));
        logger.error("스택 트레이스: {}", stackTrace);
        logger.error("========================");

        // 클라이언트에게 보낼 응답 생성
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "Invalid Argument");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("location", errorLocation);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 스택 트레이스에서 에러 발생 위치 정보 추출
     */
    private String extractErrorLocation(Exception ex, String stackTrace) {
        // 첫 번째 줄에서 클래스와 메서드 정보 추출
        String[] stackLines = stackTrace.split("\n");
        if (stackLines.length > 0) {
            String firstLine = stackLines[0];
            if (firstLine.contains("at ")) {
                // "at com.example.rag_project.service.RagService.methodName(RagService.java:123)" 형식에서 정보 추출
                String cleanLine = firstLine.replace("at ", "").trim();
                return cleanLine;
            }
        }
        
        // 예외 클래스 이름과 메시지로 대체
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
}
