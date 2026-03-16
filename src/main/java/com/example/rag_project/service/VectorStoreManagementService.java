package com.example.rag_project.service;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 벡터 저장소 관리 전담 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 벡터 저장소 관리와 관련된 모든 작업을 담당합니다:</p>
 * <ul>
 *   <li>벡터 저장소 초기화 및 상태 관리</li>
 *   <li>Redis 벡터 데이터 정리</li>
 *   <li>시스템 상태 확인 및 정보 제공</li>
 *   <li>문서 로딩 후 상태 추적</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>VectorStore를 통한 벡터 데이터 관리</li>
 *   <li>Redis 벡터 인덱스 및 데이터 정리</li>
 *   <li>시스템 초기화 상태 추적</li>
 *   <li>문서 로딩 상태 모니터링</li>
 * </ul>
 * 
 * <p><b>설정값:</b></p>
 * <ul>
 *   <li>{@code rag.documents.folder}: 문서 폴더 경로 (기본값: documents)</li>
 * </ul>
 * 
 * <p><b>의존성:</b> VectorStore, DocumentProcessingService</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreManagementService {

    private final VectorStore vectorStore;
    private final DocumentProcessingService documentProcessingService;

    @Value("${rag.documents.folder:documents}")
    private String documentsFolder;

    private boolean isInitialized = false;

    /**
     * 벡터 저장소 초기화 (Redis 데이터 정리)
     */
    public void clearStore() {
        try {
            isInitialized = false;
            
            // Redis 벡터 데이터 직접 삭제
            try {
                redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis("localhost", 6379);
                
                // 벡터 인덱스 삭제
                try {
                    Object result = jedis.sendCommand(
                        redis.clients.jedis.Protocol.Command.valueOf("FT.DROPINDEX"),
                        "vector_index".getBytes(), "DD".getBytes());
                    log.info("벡터 인덱스 삭제 완료: {}", result);
                } catch (Exception e) {
                    log.info("벡터 인덱스 삭제 실패 (이미 없거나 오류): {}", e.getMessage());
                }
                
                // 모든 벡터 관련 키 삭제
                Set<String> ragKeys = jedis.keys("rag:*");
                if (!ragKeys.isEmpty()) {
                    jedis.del(ragKeys.toArray(new String[0]));
                    log.info("RAG 관련 키 {}개 삭제 완료", ragKeys.size());
                }
                
                Set<String> embeddingKeys = jedis.keys("embedding:*");
                if (!embeddingKeys.isEmpty()) {
                    jedis.del(embeddingKeys.toArray(new String[0]));
                    log.info("Embedding 관련 키 {}개 삭제 완료", embeddingKeys.size());
                }
                
                jedis.close();
                log.info("Redis 벡터 데이터가 삭제되었습니다.");
                
            } catch (Exception e) {
                log.error("Redis 벡터 데이터 삭제 실패: {}", e.getMessage());
            }
            
            log.info("Redis Vector Store가 초기화되었습니다.");
            
        } catch (Exception e) {
            log.error("벡터 저장소 초기화 실패: {}", e.getMessage());
            throw new RuntimeException("벡터 저장소 초기화 중 오류 발생", e);
        }
    }

    /**
     * 시스템 초기화 상태 확인
     */
    public void initializeDocuments() {
        try {
            log.info("=== Redis 벡터 저장소 상태 확인 ===");
            
            List<String> redisKeys = documentProcessingService.getAllRedisDocumentKeys();
            int documentCount = redisKeys.size();
            
            if (documentCount > 0) {
                isInitialized = true;
                log.info("Redis 벡터 저장소에 {}개의 문서가 있습니다.", documentCount);
                log.info("시스템이 준비되었습니다.");
            } else {
                isInitialized = false;
                log.info("Redis 벡터 저장소에 데이터가 없습니다.");
                log.info("수동으로 데이터를 로드해주세요 (/load-from-files API 호출).");
            }
            
        } catch (Exception e) {
            log.error("Redis 벡터 저장소 상태 확인 실패: {}", e.getMessage());
            isInitialized = false;
        }
    }

    /**
     * 초기화 상태 반환
     */
    public boolean isInitialized() {
        if (isInitialized) {
            return true;
        }
        
        try {
            List<String> redisKeys = documentProcessingService.getAllRedisDocumentKeys();
            if (!redisKeys.isEmpty()) {
                log.info("Redis에 {}개의 문서가 있어 초기화된 것으로 간주합니다", redisKeys.size());
                isInitialized = true;
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 상태 확인 중 오류: {}", e.getMessage());
        }
        
        return false;
    }

    /**
     * 시스템 상태 정보 반환
     */
    public Map<String, Object> getStatusWithFiles() {
        Map<String, Object> status = new HashMap<>();
        status.put("isInitialized", isInitialized());
        
        if (isInitialized()) {
            int vectorStoreCount = 0;
            Set<String> loadedFiles = new HashSet<>();
            
            try {
                String currentDir = System.getProperty("user.dir");
                Path documentsPath = Paths.get(currentDir, documentsFolder);
                
                if (Files.exists(documentsPath)) {
                    Files.list(documentsPath)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".txt") || fileName.endsWith(".md");
                        })
                        .forEach(path -> {
                            loadedFiles.add(path.getFileName().toString());
                        });
                    
                    vectorStoreCount = loadedFiles.size() * 3;
                }
            } catch (Exception e) {
                log.error("문서 상태 확인 실패: {}", e.getMessage());
                vectorStoreCount = 0;
            }
            
            List<String> redisKeys = documentProcessingService.getAllRedisDocumentKeys();
            if (!redisKeys.isEmpty()) {
                log.info("Redis에서 {}개의 문서 키를 찾았습니다", redisKeys.size());
                
                for (String key : redisKeys) {
                    try {
                        Map<String, Object> doc = documentProcessingService.getRedisDocument(key);
                        
                        if (doc.containsKey("metadata")) {
                            Object metadata = doc.get("metadata");
                            
                            if (metadata instanceof Map) {
                                Map<?, ?> metaMap = (Map<?, ?>) metadata;
                                Object filename = metaMap.get("filename");
                                if (filename != null) {
                                    loadedFiles.add(filename.toString());
                                }
                            } else if (metadata instanceof String) {
                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> metaMap = mapper.readValue((String) metadata, Map.class);
                                    Object filename = metaMap.get("filename");
                                    if (filename != null) {
                                        loadedFiles.add(filename.toString());
                                    }
                                } catch (Exception jsonEx) {
                                    log.warn("메타데이터 JSON 파싱 실패: {}", jsonEx.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("문서 처리 실패 ({}): {}", key, e.getMessage());
                    }
                }
                
                vectorStoreCount = Math.max(vectorStoreCount, redisKeys.size());
            }
            
            status.put("loadedFiles", new ArrayList<>(loadedFiles));
            status.put("documentCount", vectorStoreCount);
            status.put("message", "문서가 로드되어 있습니다. (벡터 저장소 기준)");
        } else {
            status.put("loadedFiles", new ArrayList<>());
            status.put("documentCount", 0);
            status.put("message", "문서가 로드되지 않았습니다.");
        }
        
        return status;
    }

    /**
     * 문서 로딩 후 상태 업데이트
     */
    public void markAsInitialized() {
        isInitialized = true;
        log.info("벡터 저장소가 초기화되었음으로 표시됨");
    }

    /**
     * 파일 시스템에서 문서 로드
     */
    public Map<String, Object> loadDocumentsFromFilesystem() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 파일에서 Redis로 데이터 로드 시작 ===");
            
            clearStore();
            
            String currentDir = System.getProperty("user.dir");
            String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
            log.info("문서 경로: {}", projectDocumentsPath);
            
            documentProcessingService.loadDocumentsFromFolder(projectDocumentsPath);
            
            result.put("success", true);
            result.put("message", "파일에서 Redis로 문서 로드가 완료되었습니다.");
            result.put("documentCount", documentProcessingService.getAllRedisDocumentKeys().size());
            
        } catch (IOException e) {
            log.error("파일에서 Redis로 데이터 로드 실패: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "로드 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "오류 발생: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Redis에서 벡터 저장소로 문서 로드
     */
    public void loadDocumentsFromRedis() {
        try {
            List<Map<String, Object>> redisDocuments = documentProcessingService.getAllRedisDocuments();
            
            if (redisDocuments.isEmpty()) {
                log.info("Redis에 저장된 문서가 없습니다.");
                return;
            }
            
            List<org.springframework.ai.document.Document> documents = new ArrayList<>();
            
            for (Map<String, Object> redisDoc : redisDocuments) {
                String content = (String) redisDoc.get("content");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) redisDoc.get("metadata");
                
                if (content != null && !content.trim().isEmpty()) {
                    org.springframework.ai.document.Document document = new org.springframework.ai.document.Document(content, metadata);
                    documents.add(document);
                }
            }
            
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                markAsInitialized();
                log.info("Redis에서 {}개 문서를 벡터 저장소에 로드했습니다.", documents.size());
            }
            
        } catch (Exception e) {
            log.error("Redis 문서 로드 실패: {}", e.getMessage());
            throw new RuntimeException("Redis 문서 로드 중 오류 발생", e);
        }
    }
}
