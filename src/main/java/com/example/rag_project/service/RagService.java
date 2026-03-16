package com.example.rag_project.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * RAG 시스템 핵심 비즈니스 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 핵심 비즈니스 로직을 담당합니다:</p>
 * <ul>
 *   <li>문서 로딩 후 상태 업데이트 (비즈니스 로직)</li>
 *   <li>전문가 Service들의 기능 위임 및 조합</li>
 *   <li>레거시 API 호환성 유지</li>
 * </ul>
 * 
 * <p><b>핵심 비즈니스 로직:</b></p>
 * <ul>
 *   <li>문서 로딩 후 벡터 저장소 상태 업데이트</li>
 *   <li>예외 처리 및 로깅</li>
 * </ul>
 * 
 * <p><b>위임 기능:</b></p>
 * <ul>
 *   <li>문서 처리: DocumentProcessingService</li>
 *   <li>검색 및 답변: SearchService</li>
 *   <li>벡터 저장소 관리: VectorStoreManagementService</li>
 * </ul>
 * 
 * <p><b>의존성:</b> DocumentProcessingService, SearchService, VectorStoreManagementService</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentProcessingService documentProcessingService;
    private final SearchService searchService;
    private final VectorStoreManagementService vectorStoreService;

    // === Core Business Logic ===
    
    /**
     * 문서 로딩 후 시스템 상태 업데이트 (비즈니스 로직)
     */
    public void loadDocumentsFromFolder(String folderPath) throws IOException {
        documentProcessingService.loadDocumentsFromFolder(folderPath);
        vectorStoreService.markAsInitialized();
        log.info("문서 로드 완료: {}, 시스템 초기화됨", folderPath);
    }
    
    /**
     * 텍스트 파일 로딩 후 시스템 상태 업데이트 (비즈니스 로직)
     */
    public void loadTextFile(String filePath) throws IOException {
        documentProcessingService.loadTextFile(filePath);
        vectorStoreService.markAsInitialized();
        log.info("문서 로드 완료: {}, 시스템 초기화됨", filePath);
    }

    // === Document Processing Methods (위임) ===
    
    public Map<String, Object> saveDocumentsToRedis() throws IOException {
        return documentProcessingService.saveDocumentsToRedis();
    }

    public Map<String, Object> saveDocumentsFromFolderToRedisWithDuplicateCheck(String folderPath) throws IOException {
        return documentProcessingService.saveDocumentsFromFolderToRedisWithDuplicateCheck(folderPath);
    }

    public List<String> getAllRedisDocumentKeys() {
        return documentProcessingService.getAllRedisDocumentKeys();
    }

    public Map<String, Object> getRedisDocument(String key) {
        return documentProcessingService.getRedisDocument(key);
    }

    public List<Map<String, Object>> getAllRedisDocuments() {
        return documentProcessingService.getAllRedisDocuments();
    }

    // === Search Methods (위임) ===
    
    public String searchAndAnswer(String query) {
        return searchService.searchAndAnswer(query);
    }

    public Map<String, Object> searchAndAnswerWithSources(String query) {
        return searchService.searchAndAnswerWithSources(query);
    }

    // === Vector Store Management Methods (위임) ===
    
    public void clearStore() {
        vectorStoreService.clearStore();
    }

    public void initializeDocuments() {
        vectorStoreService.initializeDocuments();
    }

    public boolean isInitialized() {
        return vectorStoreService.isInitialized();
    }

    public Map<String, Object> getStatusWithFiles() {
        return vectorStoreService.getStatusWithFiles();
    }

    // === Legacy Methods for Compatibility (위임) ===
    
    public Map<String, Object> loadDocumentsFromFilesystem() {
        return vectorStoreService.loadDocumentsFromFilesystem();
    }

    public void loadDocumentsFromRedis() {
        vectorStoreService.loadDocumentsFromRedis();
    }
}
