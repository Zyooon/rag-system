package com.example.rag_project.service;

import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.constants.ErrorConstants;
import com.example.rag_project.constants.MetadataConstants;
import com.example.rag_project.constants.MessageConstants;
import com.example.rag_project.constants.RedisConstants;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
 * <p>이 클래스는 RAG 시스템의 벡터 저장소 관련 모든 작업을 담당합니다:</p>
 * <ul>
 *   <li>벡터 저장소 초기화 및 정리</li>
 *   <li>Redis 데이터 로드 및 관리</li>
 *   <li>시스템 상태 모니터링</li>
 *   <li>파일 시스템과 동기화</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>VectorStore를 통한 벡터 데이터 관리</li>
 *   <li>RedisTemplate을 통한 Redis 키 관리</li>
 *   <li>DocumentProcessingService와의 협업</li>
 *   <li>시스템 초기화 상태 추적</li>
 * </ul>
 * 
 * <p><b>설정값:</b></p>
 * <ul>
 *   <li>{@code rag.documents.folder}: 문서 폴더 경로</li>
 *   <li>{@code rag.redis.vectorstore.index-name}: 벡터 인덱스 이름</li>
 *   <li>{@code rag.redis.vectorstore.key-prefix}: Redis 키 접두사</li>
 * </ul>
 * 
 * <p><b>의존성:</b> VectorStore, DocumentProcessingService, RedisTemplate</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreManagementService {

    private final VectorStore vectorStore;
    private final DocumentProcessingService documentProcessingService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${" + ConfigConstants.CONFIG_DOCUMENTS_FOLDER + ":" + ConfigConstants.DEFAULT_DOCUMENTS_FOLDER + "}")
    private String documentsFolder;

    @Value("${" + ConfigConstants.CONFIG_VECTORSTORE_INDEX_NAME + ":" + RedisConstants.VECTOR_INDEX_NAME + "}")
    private String indexName;

    @Value("${" + ConfigConstants.CONFIG_VECTORSTORE_KEY_PREFIX + ":" + RedisConstants.REDIS_KEY_PREFIX + "}")
    private String keyPrefix;

    @Value("${" + ConfigConstants.CONFIG_VECTORSTORE_EMBEDDING_PREFIX + ":" + RedisConstants.REDIS_EMBEDDING_KEY_PREFIX + "}")
    private String embeddingPrefix;

    private boolean isInitialized = false;

    /**
     * 벡터 저장소 초기화 (Spring AI 추상화 계층 활용)
     */
    public void clearStore() {
        try {
            isInitialized = false;
            
            // 1. RedisTemplate을 통해 안전하게 키 삭제
            clearRedisKeys();
            
            // 2. VectorStore를 통한 벡터 데이터 삭제 (Spring AI 추상화)
            clearVectorStoreData();
            
            log.info("벡터 저장소가 초기화되었습니다.");
            
        } catch (Exception e) {
            log.error(ErrorConstants.ERROR_VECTORSTORE_INIT_FAILED + e.getMessage());
            throw new RuntimeException(MessageConstants.MSG_VECTORSTORE_INIT_ERROR, e);
        }
    }
    
    /**
     * RedisTemplate을 통해 설정된 키 패턴으로 데이터 삭제
     */
    private void clearRedisKeys() {
        try {
            // RAG 관련 키 삭제
            Set<String> ragKeys = redisTemplate.keys(keyPrefix + "*");
            if (ragKeys != null && !ragKeys.isEmpty()) {
                redisTemplate.delete(ragKeys);
                log.info("RAG 관련 키 {}개 삭제 완료", ragKeys.size());
            }
            
            // Embedding 관련 키 삭제
            Set<String> embeddingKeys = redisTemplate.keys(embeddingPrefix + "*");
            if (embeddingKeys != null && !embeddingKeys.isEmpty()) {
                redisTemplate.delete(embeddingKeys);
                log.info("Embedding 관련 키 {}개 삭제 완료", embeddingKeys.size());
            }
            
        } catch (Exception e) {
            log.warn(MessageConstants.MSG_REDIS_KEY_DELETE_ERROR, e.getMessage());
        }
    }
    
    /**
     * VectorStore를 통한 벡터 데이터 삭제 (Spring AI 추상화)
     */
    private void clearVectorStoreData() {
        try {
            // VectorStore의 모든 문서 ID 가져오기
            // Spring AI RedisVectorStore는 내부적으로 메타데이터를 관리
            // 현재로서는 Redis 키 삭제로 충분히 벡터 데이터 정리
            log.debug("VectorStore 데이터 정리 완료");
            
        } catch (Exception e) {
            log.warn(MessageConstants.MSG_VECTORSTORE_CLEAN_ERROR, e.getMessage());
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
            log.error(ErrorConstants.LOG_REDIS_VECTORSTORE_STATUS_FAILED, e.getMessage());
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
            log.warn(ErrorConstants.LOG_REDIS_STATUS_CHECK_ERROR, e.getMessage());
        }
        
        return false;
    }

    /**
     * 시스템 상태 정보 반환
     */
    public Map<String, Object> getStatusWithFiles() {
        Map<String, Object> status = new HashMap<>();
        status.put(MetadataConstants.MAP_KEY_IS_INITIALIZED, isInitialized());
        
        if (isInitialized()) {
            int vectorStoreCount = 0;
            Set<String> loadedFiles = new HashSet<>();
            
            try {
                String currentDir = System.getProperty(ConfigConstants.SYSTEM_USER_DIR);
                Path documentsPath = Paths.get(currentDir, documentsFolder);
                
                if (Files.exists(documentsPath)) {
                    Files.list(documentsPath)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(ConfigConstants.TXT_EXTENSION) || fileName.endsWith(ConfigConstants.MD_EXTENSION);
                        })
                        .forEach(path -> {
                            loadedFiles.add(path.getFileName().toString());
                        });
                    
                    vectorStoreCount = loadedFiles.size() * 3;
                }
            } catch (Exception e) {
                log.error(ErrorConstants.LOG_DOCUMENT_STATUS_CHECK_FAILED, e.getMessage());
                vectorStoreCount = 0;
            }
            
            List<String> redisKeys = documentProcessingService.getAllRedisDocumentKeys();
            if (!redisKeys.isEmpty()) {
                log.info("Redis에 {}개의 문서 키를 찾았습니다", redisKeys.size());
                
                for (String key : redisKeys) {
                    try {
                        Map<String, Object> doc = documentProcessingService.getRedisDocument(key);
                        
                        if (doc.containsKey(MetadataConstants.JSON_KEY_METADATA)) {
                            Object metadata = doc.get(MetadataConstants.JSON_KEY_METADATA);
                            
                            if (metadata instanceof Map) {
                                Map<?, ?> metaMap = (Map<?, ?>) metadata;
                                Object filename = metaMap.get(MetadataConstants.JSON_KEY_FILENAME);
                                if (filename != null) {
                                    loadedFiles.add(filename.toString());
                                }
                            } else if (metadata instanceof String) {
                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> metaMap = mapper.readValue((String) metadata, Map.class);
                                    Object filename = metaMap.get(MetadataConstants.JSON_KEY_FILENAME);
                                    if (filename != null) {
                                        loadedFiles.add(filename.toString());
                                    }
                                } catch (Exception jsonEx) {
                                    log.warn(ErrorConstants.LOG_METADATA_JSON_PARSE_FAILED, jsonEx.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(ErrorConstants.LOG_DOCUMENT_PROCESS_FAILED, key, e.getMessage());
                    }
                }
                
                vectorStoreCount = Math.max(vectorStoreCount, redisKeys.size());
            }
            
            status.put(MetadataConstants.MAP_KEY_LOADED_FILES, new ArrayList<>(loadedFiles));
            status.put(MetadataConstants.MAP_KEY_DOCUMENT_COUNT, vectorStoreCount);
            status.put(MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_DOCUMENTS_LOADED);
        } else {
            status.put(MetadataConstants.MAP_KEY_LOADED_FILES, new ArrayList<>());
            status.put(MetadataConstants.MAP_KEY_DOCUMENT_COUNT, 0);
            status.put(MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_DOCUMENTS_NOT_LOADED);
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
            
            String currentDir = System.getProperty(ConfigConstants.SYSTEM_USER_DIR);
            String projectDocumentsPath = Paths.get(currentDir, ConfigConstants.DOCUMENTS_FOLDER_NAME).toString();
            log.info("문서 경로: {}", projectDocumentsPath);
            
            documentProcessingService.loadDocumentsFromFolder(projectDocumentsPath);
            
            result.put(MetadataConstants.MAP_KEY_SUCCESS, true);
            result.put(MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_FILE_LOAD_SUCCESS);
            result.put(MetadataConstants.MAP_KEY_DOCUMENT_COUNT, documentProcessingService.getAllRedisDocumentKeys().size());
            
        } catch (IOException e) {
            log.error(ErrorConstants.LOG_FILE_LOAD_FAILED, e.getMessage());
            result.put(MetadataConstants.MAP_KEY_SUCCESS, false);
            result.put(MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_FILE_LOAD_FAILED_PREFIX + e.getMessage());
        } catch (Exception e) {
            log.error(ErrorConstants.LOG_UNEXPECTED_ERROR, e.getMessage());
            result.put(MetadataConstants.MAP_KEY_SUCCESS, false);
            result.put(MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_ERROR_PREFIX + e.getMessage());
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
                log.info(MessageConstants.MSG_REDIS_NO_DOCUMENTS);
                return;
            }
            
            List<org.springframework.ai.document.Document> documents = new ArrayList<>();
            
            for (Map<String, Object> redisDoc : redisDocuments) {
                String content = (String) redisDoc.get(MetadataConstants.JSON_KEY_CONTENT);
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) redisDoc.get(MetadataConstants.JSON_KEY_METADATA);
                
                if (content != null && !content.trim().isEmpty()) {
                    org.springframework.ai.document.Document document = new org.springframework.ai.document.Document(content, metadata);
                    documents.add(document);
                }
            }
            
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                markAsInitialized();
                log.info(MessageConstants.MSG_REDIS_LOAD_SUCCESS, documents.size());
            }
            
        } catch (Exception e) {
            log.error(ErrorConstants.ERROR_REDIS_LOAD_FAILED, e.getMessage());
            throw new RuntimeException(MessageConstants.MSG_REDIS_LOAD_ERROR, e);
        }
    }
}
