package com.example.rag_project.controller;

import com.example.rag_project.dto.RagRequest;
import com.example.rag_project.dto.RagResponse;
import com.example.rag_project.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/initialize")
    public ResponseEntity<RagResponse> initializeDocuments() {
        try {
            ragService.initializeDocuments();
            return ResponseEntity.ok(RagResponse.success("문서들이 자동으로 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("초기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<RagResponse> query(@RequestBody RagRequest request) {
        try {
            String answer = ragService.searchAndAnswer(request.getQuery());
            return ResponseEntity.ok(RagResponse.success(answer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("질의응답 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<RagResponse> getStatus() {
        try {
            boolean isInitialized = ragService.isInitialized();
            String message = isInitialized ? "문서가 로드되어 있습니다." : "문서가 로드되지 않았습니다.";
            return ResponseEntity.ok(RagResponse.success(message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("상태 확인 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<RagResponse> saveDocumentsToRedis() {
        try {
            int savedCount = ragService.saveDocumentsToRedis();
            return ResponseEntity.ok(RagResponse.success("총 " + savedCount + "개의 문서 조각이 Redis에 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 저장 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/redis/clear")
    public ResponseEntity<RagResponse> clearAllRedisDocuments() {
        try {
            int deletedCount = ragService.clearAllRedisDocuments();
            return ResponseEntity.ok(RagResponse.success("Redis 문서 삭제 완료: " + deletedCount + "개"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 문서 삭제 실패: " + e.getMessage()));
        }
    }
}
