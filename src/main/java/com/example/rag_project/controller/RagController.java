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

    @PostMapping("/load")
    public ResponseEntity<RagResponse> loadTextFile(@RequestBody RagRequest request) {
        try {
            ragService.loadTextFile(request.getFilePath());
            return ResponseEntity.ok(RagResponse.success("파일이 성공적으로 로드되었습니다: " + request.getFilePath()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("파일 로드 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/load-folder")
    public ResponseEntity<RagResponse> loadDocumentsFromFolder(@RequestBody RagRequest request) {
        try {
            ragService.loadDocumentsFromFolder(request.getFilePath());
            return ResponseEntity.ok(RagResponse.success("폴더의 문서들이 성공적으로 로드되었습니다: " + request.getFilePath()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("폴더 로드 실패: " + e.getMessage()));
        }
    }

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

    @DeleteMapping("/clear")
    public ResponseEntity<RagResponse> clearStore() {
        try {
            ragService.clearStore();
            return ResponseEntity.ok(RagResponse.success("벡터 저장소가 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("초기화 실패: " + e.getMessage()));
        }
    }
}
