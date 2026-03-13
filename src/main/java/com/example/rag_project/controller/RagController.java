package com.example.rag_project.controller;

import com.example.rag_project.dto.RagRequest;
import com.example.rag_project.dto.RagResponse;
import com.example.rag_project.dto.SourceInfo;
import com.example.rag_project.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping(value = "/ask", produces = "application/json; charset=UTF-8")
    public ResponseEntity<RagResponse> query(@RequestBody RagRequest request) {
        try {
            // Redis 데이터만 사용하여 답변 생성
            Map<String, Object> result = ragService.searchAndAnswerWithSources(request.getQuery());
            String answer = (String) result.get("answer");
            SourceInfo sources = (SourceInfo) result.get("sources");
            
            return ResponseEntity.ok(RagResponse.success(answer, null, sources));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 조회 실패: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<RagResponse> getStatus() {
        try {
            java.util.Map<String, Object> status = ragService.getStatusWithFiles();
            status.put("redis_connection", "connected");
            status.put("vector_store_type", "simple_with_redis_backup");
            
            return ResponseEntity.ok(RagResponse.success("Redis 연결 상태 확인", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 상태 확인 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/storage")
    public ResponseEntity<RagResponse> clearRedisVectorStore() {
        try {
            // Redis 벡터 저장소 초기화 (RedisVectorStore가 직접 처리)
            ragService.clearStore();
            return ResponseEntity.ok(RagResponse.success("Redis Vector Store 삭제 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis Vector Store 삭제 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/storage")
    public ResponseEntity<RagResponse> buildRedisVectorStore() {
        try {
            java.util.Map<String, Object> result = ragService.saveDocumentsToRedis();
            String message = result.get("message").toString();
            
            return ResponseEntity.ok(RagResponse.success(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis Vector Store 구축 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/reload")
    public ResponseEntity<RagResponse> reloadDocuments() {
        try {
            // 벡터 저장소 초기화 후 다시 로드
            ragService.clearStore();
            ragService.initializeDocuments();
            return ResponseEntity.ok(RagResponse.success("문서가 다시 로드되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("문서 재로드 실패: " + e.getMessage()));
        }
    }
}
