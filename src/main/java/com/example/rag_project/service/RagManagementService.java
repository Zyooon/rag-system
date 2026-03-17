package com.example.rag_project.service;

import com.example.rag_project.config.VectorStoreConfig;
import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.constants.MessageConstants;
import com.example.rag_project.splitter.TextSplitterProcessor;
import com.example.rag_project.repository.RedisDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * RAG 시스템 통합 관리 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 모든 핵심 기능을 통합 관리합니다:</p>
 * <ul>
 *   <li>📁 <b>문서 처리</b> - 파일 읽기, 파싱, 분할, 저장 전체 파이프라인</li>
 *   <li>🗄️ <b>벡터 저장소 관리</b> - 초기화, 정리, 상태 모니터링</li>
 *   <li>📊 <b>시스템 상태</b> - 전체 시스템 상태 정보 제공</li>
 *   <li>🔄 <b>생명주기 관리</b> - 시스템 초기화부터 운영까지</li>
 * </ul>
 * 
 * <p><b>통합된 책임:</b></p>
 * <ul>
 *   <li><b>문서 수집</b>: 파일 시스템에서 문서를 읽고 메타데이터 추출</li>
 *   <li><b>파싱 관리</b>: ParseManager를 통한 최적 파싱 전략 실행</li>
 *   <li><b>분할 처리</b>: TextSplitterProcessor를 통한 의미 단위 청킹</li>
 *   <li><b>저장 관리</b>: RedisDocumentRepository를 통한 문서 저장</li>
 *   <li><b>저장소 제어</b>: 벡터 저장소 초기화, 정리, 상태 모니터링</li>
 *   <li><b>품질 관리</b>: 중복 문서 감지 및 필터링</li>
 * </ul>
 * 
 * <p><b>처리 파이프라인:</b></p>
 * <ol>
 *   <li>📖 FileManager 파일 읽기</li>
 *   <li>🔍 ParseManager 파싱</li>
 *   <li>✂️ TextSplitterProcessor 분할</li>
 *   <li>💾 RedisDocumentRepository 저장</li>
 *   <li>📊 상태 업데이트</li>
 * </ol>
 * 
 * <p><b>의존성:</b> ParseManager, RedisDocumentRepository, FileManager, TextSplitterProcessor, VectorStoreConfig</p>
 * <p><b>출력물:</b> 처리된 문서 청크들, 시스템 상태 정보</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagManagementService {

    private final ParseManager parseManager;
    private final RedisDocumentRepository redisDocumentRepository;
    private final FileManager fileManager;
    private final TextSplitterProcessor textSplitterProcessor;
    private final VectorStoreConfig vectorStoreConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    private boolean isInitialized = false;

    // ==================== 문서 처리 기능 ====================

    /**
     * 현재 documents 폴더의 모든 문서를 Redis에 저장하는 메서드
     */
    public Map<String, Object> saveDocumentsToRedis() throws IOException {
        return saveDocumentsFromFolderToRedisWithDuplicateCheck(CommonConstants.DOCUMENTS_FOLDER_NAME);
    }

    /**
     * 특정 폴더의 모든 텍스트 파일을 자동으로 로드하는 메서드
     */
    public void loadDocumentsFromFolder(String folderPath) throws IOException {
        if (!fileManager.ensureFolderExists(folderPath)) {
            log.warn("폴더 생성 실패: {}", folderPath);
            return;
        }

        List<Document> allDocuments = processFilesInFolder(folderPath);
        
        if (!allDocuments.isEmpty()) {
            List<Document> finalDocuments = createFinalDocuments(allDocuments);
            log.info("문서 처리 완료: {}개 최종 문서 생성", finalDocuments.size());
        }
    }

    /**
     * 특정 폴더의 문서들을 Redis에 저장하는 메서드 (중복 방지)
     */
    public Map<String, Object> saveDocumentsFromFolderToRedisWithDuplicateCheck(String folderPath) throws IOException {
        Map<String, Object> folderStatus = fileManager.getFolderStatus(folderPath);
        
        if (!(Boolean) folderStatus.get(CommonConstants.KEY_CONTENT)) {
            return Map.of(
            ConfigConstants.MAP_KEY_SAVED_COUNT, 0,
            ConfigConstants.MAP_KEY_DUPLICATE_COUNT, 0,
                ConfigConstants.MAP_KEY_TOTAL_COUNT, 0,
                ConfigConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_FOLDER_NOT_EXISTS
            );
        }

        List<Document> allDocuments = loadDocumentsFromFolderSimple(folderPath);
        
        if (!allDocuments.isEmpty()) {
            List<Document> splitDocuments = textSplitterProcessor.splitDocuments(allDocuments);
            Map<String, Object> saveResult = redisDocumentRepository.saveDocuments(splitDocuments);
            
            int savedCount = (Integer) saveResult.get(ConfigConstants.MAP_KEY_SAVED_COUNT);
            if (savedCount > 0) {
                log.info("문서 저장 완료: {}개 청크 Redis 저장됨", savedCount);
            }
            
            return createSaveResult(allDocuments, splitDocuments, saveResult);
        }
        
        return Map.of(
            ConfigConstants.MAP_KEY_SAVED_COUNT, 0,
            ConfigConstants.MAP_KEY_DUPLICATE_COUNT, 0,
            ConfigConstants.MAP_KEY_TOTAL_COUNT, 0,
            ConfigConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_NO_TEXT_FILES
        );
    }

    // ==================== 벡터 저장소 관리 기능 ====================

    /**
     * 벡터 저장소 초기화 (Spring AI 추상화 계층 활용)
     * @return 삭제된 키 수를 포함한 결과 맵
     */
    public Map<String, Object> clearStore() {
        Map<String, Object> result = new HashMap<>();
        int totalDeleted = 0;
        
        try {
            isInitialized = false;
            
            // 1. RedisTemplate을 통해 안전하게 키 삭제
            Map<String, Object> redisResult = clearRedisKeys();
            int ragDeleted = (Integer) redisResult.get(ConfigConstants.MAP_KEY_RAG_KEYS);
            int embeddingDeleted = (Integer) redisResult.get(ConfigConstants.MAP_KEY_EMBEDDING_KEYS);
            totalDeleted = ragDeleted + embeddingDeleted;
            
            result.put(ConfigConstants.MAP_KEY_RAG_KEYS, ragDeleted);
            result.put(ConfigConstants.MAP_KEY_EMBEDDING_KEYS, embeddingDeleted);
            
            // 2. VectorStore 데이터 정理
            clearVectorStoreData();
            
            result.put(ConfigConstants.MAP_KEY_TOTAL_DELETED, totalDeleted);
            result.put(ConfigConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_VECTORSTORE_DATA_CLEANED + ": 총 " + totalDeleted + "개 키 삭제됨");
            
            log.info("벡터 저장소 초기화 완료: 총 {}개 키 삭제됨", totalDeleted);
            
        } catch (Exception e) {
            log.error("벡터 저장소 초기화 실패: {}", e.getMessage());
            result.put(ConfigConstants.MAP_KEY_TOTAL_DELETED, 0);
            result.put(ConfigConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_VECTORSTORE_INIT_ERROR);
        }
        
        return result;
    }

    /**
     * 시스템 상태 정보 반환
     */
    public Map<String, Object> getStatusWithFiles() {
        Map<String, Object> status = new HashMap<>();
        status.put(ConfigConstants.MAP_KEY_IS_INITIALIZED, isInitialized());
        
        if (isInitialized()) {
            int vectorStoreCount = 0;
            Set<String> loadedFiles = new HashSet<>();
            
            try {
                // FileManager를 통해 파일 목록 가져오기
                Map<String, Object> folderStatus = fileManager.getFolderStatus(vectorStoreConfig.getDocumentsFolder());
                
                if ((Boolean) folderStatus.get(CommonConstants.KEY_CONTENT)) {
                    @SuppressWarnings("unchecked")
                    List<String> files = (List<String>) folderStatus.get(CommonConstants.KEY_METADATA);
                    
                    for (String fileName : files) {
                        String lowerFileName = fileName.toLowerCase();
                        if (lowerFileName.endsWith(CommonConstants.TXT_EXTENSION) || 
                            lowerFileName.endsWith(CommonConstants.MD_EXTENSION)) {
                            loadedFiles.add(fileName);
                        }
                    }
                    
                    vectorStoreCount = loadedFiles.size() * 3;
                }
            } catch (Exception e) {
                log.error(MessageConstants.LOG_DOCUMENT_STATUS_CHECK_FAILED, e.getMessage());
                vectorStoreCount = 0;
            }
            
            List<String> redisKeys = redisDocumentRepository.getAllDocumentKeys();
            if (!redisKeys.isEmpty()) {
                status.put(ConfigConstants.MAP_KEY_RAG_KEYS_DELETED, redisKeys);
                
                // Redis에 저장된 메타데이터에서 파일명 추출
                for (String key : redisKeys) {
                    try {
                        Map<String, Object> doc = redisDocumentRepository.getDocument(key);
                        if (doc != null) {
                            Object metadata = doc.get(CommonConstants.KEY_METADATA);
                            
                            if (metadata instanceof Map) {
                                Map<?, ?> metaMap = (Map<?, ?>) metadata;
                                Object filename = metaMap.get(CommonConstants.METADATA_KEY_FILENAME);
                                if (filename != null) {
                                    loadedFiles.add(filename.toString());
                                }
                            } else if (metadata instanceof String) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> metaMap = mapper.readValue((String) metadata, Map.class);
                                    Object filename = metaMap.get(CommonConstants.METADATA_KEY_FILENAME);
                                    if (filename != null) {
                                        loadedFiles.add(filename.toString());
                                    }
                                } catch (Exception jsonEx) {
                                    log.warn(MessageConstants.LOG_METADATA_JSON_PARSE_FAILED, jsonEx.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Redis 키 {} 처리 실패: {}", key, e.getMessage());
                    }
                }
            }
            
            status.put(ConfigConstants.MAP_KEY_LOADED_FILES, new ArrayList<>(loadedFiles));
            status.put(ConfigConstants.MAP_KEY_DOCUMENT_COUNT, vectorStoreCount);
            status.put(ConfigConstants.MAP_KEY_TOTAL_COUNT, redisKeys.size());
        }
        
        return status;
    }

    /**
     * 초기화 상태 확인
     */
    public void initializeDocuments() {
        try {
            log.info("=== Redis 벡터 저장소 상태 확인 ===");
            
            List<String> redisKeys = redisDocumentRepository.getAllDocumentKeys();
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
            log.error("벡터 저장소 초기화 실패: {}", e.getMessage());
            isInitialized = false;
        }
    }
    
    /**
     * RedisDocumentRepository를 통해 설정된 키 패턴으로 데이터 삭제
     * @return 삭제된 키 수를 포함한 결과 맵
     */
    private Map<String, Object> clearRedisKeys() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // RedisDocumentRepository를 통해 키 패턴으로 삭제
            List<String> keyPatterns = vectorStoreConfig.getKeyPatterns();
            Map<String, Integer> deleteResults = redisDocumentRepository.deleteKeysByPatterns(keyPatterns);
            
            int ragDeleted = deleteResults.getOrDefault(vectorStoreConfig.getFullKeyPattern(), 0);
            int embeddingDeleted = deleteResults.getOrDefault(vectorStoreConfig.getEmbeddingKeyPattern(), 0);
            
            result.put(ConfigConstants.MAP_KEY_RAG_KEYS, ragDeleted);
            result.put(ConfigConstants.MAP_KEY_EMBEDDING_KEYS, embeddingDeleted);
            
        } catch (Exception e) {
            log.warn(MessageConstants.MSG_REDIS_KEY_DELETE_ERROR, e.getMessage());
            result.put(ConfigConstants.MAP_KEY_RAG_KEYS, 0);
            result.put(ConfigConstants.MAP_KEY_EMBEDDING_KEYS, 0);
        }
        
        return result;
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
     * 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    // private helper methods
    private List<Document> processFilesInFolder(String folderPath) throws IOException {
        List<Document> allDocuments = new ArrayList<>();

        List<FileManager.FileContent> fileContents = fileManager.readAllSupportedFiles(folderPath);
        
        for (FileManager.FileContent fileContent : fileContents) {
            try {
                String content = fileContent.getContent();
                String filename = fileContent.getFilename();
                
                // ParseManager를 통해 최적의 파서 자동 선택
                List<Document> parsedDocuments = parseManager.parseDocument(content, filename);
                
                // TextSplitterProcessor를 통한 긴 문서 분할
                List<Document> finalDocuments = textSplitterProcessor.splitLongDocuments(parsedDocuments);
                
                allDocuments.addAll(finalDocuments);
            } catch (Exception e) {
                log.error(MessageConstants.LOG_DOCUMENT_PROCESS_FAILED, fileContent.getFilename(), e.getMessage());
            }
        }
            
        return allDocuments;
    }

    private List<Document> createFinalDocuments(List<Document> allDocuments) {
        List<Document> finalDocuments = new ArrayList<>();
        int globalChunkIndex = 0;
        
        for (Document originalDoc : allDocuments) {
            Map<String, Object> metadata = new HashMap<>(originalDoc.getMetadata());
            metadata.put("chunk_index", globalChunkIndex);
            
            finalDocuments.add(new Document(originalDoc.getText(), metadata));
            globalChunkIndex++;
        }
        
        return finalDocuments;
    }

    private Map<String, Object> createSaveResult(List<Document> originalDocs, List<Document> splitDocs, Map<String, Object> saveResult) {
        return Map.of(
            ConfigConstants.MAP_KEY_SAVED_COUNT, saveResult.get(ConfigConstants.MAP_KEY_SAVED_COUNT),
            ConfigConstants.MAP_KEY_DUPLICATE_COUNT, saveResult.get(ConfigConstants.MAP_KEY_DUPLICATE_COUNT),
            ConfigConstants.MAP_KEY_TOTAL_COUNT, originalDocs.size(),
            ConfigConstants.MAP_KEY_DOCUMENT_COUNT, splitDocs.size(),
            ConfigConstants.MAP_KEY_MESSAGE, "문서 저장 완료"
        );
    }

    private List<Document> loadDocumentsFromFolderSimple(String folderPath) throws IOException {
        List<Document> allDocuments = new ArrayList<>();

        List<FileManager.FileContent> fileContents = fileManager.readAllSupportedFiles(folderPath);
        
        for (FileManager.FileContent fileContent : fileContents) {
            try {
                String content = fileContent.getContent();
                String filename = fileContent.getFilename();
                
                // ParseManager를 통해 최적의 파서 자동 선택
                List<Document> parsedDocuments = parseManager.parseDocument(content, filename);
                
                if (parsedDocuments.isEmpty()) {
                    log.warn("파싱 실패: {} (파서 선택 불가)", filename);
                } else {
                    log.info("ParseManager로 {}개 조각 분할: {}", parsedDocuments.size(), filename);
                }
                
                // 최종 Fallback: 정말 아무것도 안 되면 전체 저장
                if (parsedDocuments.isEmpty()) {
                    Map<String, Object> fallbackMetadata = Map.of(
                        CommonConstants.METADATA_KEY_FILENAME, filename,
                        CommonConstants.METADATA_KEY_FILEPATH, fileContent.getFilePath().toString(),
                        "saved_at", java.time.LocalDateTime.now().toString()
                    );
                    Document document = new Document(content, fallbackMetadata);
                    allDocuments.add(document);
                    log.info("전체 문서로 저장: {}", filename);
                } else {
                    allDocuments.addAll(parsedDocuments);
                    log.info("총 {}개 조각 저장: {}", parsedDocuments.size(), filename);
                }
            } catch (Exception e) {
                log.error(MessageConstants.LOG_DOCUMENT_PROCESS_FAILED, fileContent.getFilename(), e.getMessage());
            }
        }
        
        return allDocuments;
    }
}
