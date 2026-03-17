package com.example.rag_project.controller;

import com.example.rag_project.dto.RagResponse;
import com.example.rag_project.service.RagManagementService;
import com.example.rag_project.constants.ConfigConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG 시스템 관리 컨트롤러
 * 
 * <p>이 컨트롤러는 RAG 시스템의 관리 관련 API를 담당합니다:</p>
 * <ul>
 *   <li>시스템 상태 조회</li>
 *   <li>문서 저장 및 초기화</li>
 *   <li>벡터 저장소 관리</li>
 * </ul>
 * 
 * <p><b>검색 기능:</b> 검색은 <code>/api/search/ask</code> 엔드포인트를 사용하세요.</p>
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagManagementService ragManagementService;

    /**
     * RAG 시스템 상태 조회 엔드포인트
     * 
     * @return 시스템 상태 정보
     */
    @GetMapping
    public ResponseEntity<RagResponse> getStatus() {
        try {
            java.util.Map<String, Object> status = ragManagementService.getStatusWithFiles();
            status.put(ConfigConstants.MAP_KEY_REDIS_CONNECTION, ConfigConstants.REDIS_CONNECTION_CONNECTED);
            status.put(ConfigConstants.MAP_KEY_VECTOR_STORE_TYPE, ConfigConstants.VECTORSTORE_TYPE_SIMPLE_REDIS_BACKUP);
            
            return ResponseEntity.ok(RagResponse.success(ConfigConstants.MSG_REDIS_CONNECTION_CHECK, status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error(ConfigConstants.MSG_REDIS_STATUS_CHECK_FAILED + e.getMessage()));
        }
    }

    @DeleteMapping("/documents")
    public ResponseEntity<RagResponse> clearRedisVectorStore() {
        try {
            // Redis 벡터 저장소 초기화 (RedisVectorStore가 직접 처리)
            Map<String, Object> result = ragManagementService.clearStore();
            
            String message = String.format(ConfigConstants.MSG_REDIS_VECTORSTORE_DELETE_COMPLETE, 
                result.get(ConfigConstants.MAP_KEY_TOTAL_DELETED), result.get(ConfigConstants.MAP_KEY_RAG_KEYS_DELETED), result.get(ConfigConstants.MAP_KEY_EMBEDDING_KEYS_DELETED));
            
            return ResponseEntity.ok(RagResponse.success(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error(ConfigConstants.MSG_REDIS_VECTORSTORE_DELETE_FAILED + e.getMessage()));
        }
    }

    @PostMapping("/documents")
    public ResponseEntity<RagResponse> buildRedisVectorStore() {
        try {
            java.util.Map<String, Object> result = ragManagementService.saveDocumentsToRedis();
            String message = result.get(ConfigConstants.MAP_KEY_MESSAGE).toString();
            
            return ResponseEntity.ok(RagResponse.success(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error(ConfigConstants.MSG_REDIS_VECTORSTORE_BUILD_FAILED + e.getMessage()));
        }
    }

    @PutMapping("/documents/reload")
    public ResponseEntity<RagResponse> reloadDocuments() {
        try {
            // 벡터 저장소 초기화 후 다시 로드
            Map<String, Object> clearResult = ragManagementService.clearStore();
            ragManagementService.initializeDocuments();
            
            String message = String.format(ConfigConstants.MSG_DOCUMENTS_RELOADED, clearResult.get(ConfigConstants.MAP_KEY_TOTAL_DELETED));
            
            return ResponseEntity.ok(RagResponse.success(message, clearResult));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error(ConfigConstants.MSG_DOCUMENT_RELOAD_FAILED + e.getMessage()));
        }
    }

}
