package com.example.rag_project.controller;

import com.example.rag_project.dto.RagRequest;
import com.example.rag_project.dto.RagResponse;
import com.example.rag_project.dto.SourceInfo;
import com.example.rag_project.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/sync")
    public ResponseEntity<RagResponse> initializeDocuments() {
        try {
            ragService.initializeDocuments();
            return ResponseEntity.ok(RagResponse.success("문서들이 자동으로 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("초기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> query(@RequestBody RagRequest request) {
        try {
            // 항상 출처 정보 포함
            Map<String, Object> result = ragService.searchAndAnswerWithSources(request.getQuery());
            String answer = (String) result.get("answer");
            @SuppressWarnings("unchecked")
            List<SourceInfo> sources = (List<SourceInfo>) result.get("sources");
            
            return ResponseEntity.ok(RagResponse.success(answer, null, sources));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("질의응답 실패: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<RagResponse> getStatus() {
        try {
            java.util.Map<String, Object> status = ragService.getStatusWithFiles();
            return ResponseEntity.ok(RagResponse.success(status.get("message").toString(), status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("상태 확인 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/storage")
    public ResponseEntity<RagResponse> saveDocumentsToRedis() {
        try {
            java.util.Map<String, Object> result = ragService.saveDocumentsToRedis();
            String message = result.get("message").toString();
            
            return ResponseEntity.ok(RagResponse.success(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 저장 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/storage")
    public ResponseEntity<RagResponse> clearAllRedisDocuments() {
        try {
            int deletedCount = ragService.clearAllRedisDocuments();
            return ResponseEntity.ok(RagResponse.success("Redis 문서 삭제 완료: " + deletedCount + "개"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("Redis 문서 삭제 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/memory")
    public ResponseEntity<RagResponse> clearVectorStore() {
        try {
            ragService.clearStore();
            return ResponseEntity.ok(RagResponse.success("벡터 저장소가 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("벡터 저장소 초기화 실패: " + e.getMessage()));
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
