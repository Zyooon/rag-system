package com.example.rag_project.config;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 벡터 저장소 설정값 관리 클래스
 * 
 * <p>이 클래스는 벡터 저장소 관련 모든 설정값을 관리합니다:</p>
 * <ul>
 *   <li><b>문서 폴더</b> - 문서가 저장된 폴더 경로</li>
 *   <li><b>Redis 키</b> - Redis 키 접두사 설정</li>
 *   <li><b>인덱스</b> - 벡터 인덱스 이름 설정</li>
 *   <li><b>임베딩</b> - 임베딩 관련 설정</li>
 * </ul>
 * 
 * <p><b>설정 카테고리:</b></p>
 * <ul>
 *   <li><b>파일 시스템</b>: 문서 폴더 경로</li>
 *   <li><b>Redis 설정</b>: 키 접두사, 임베딩 키</li>
 *   <li><b>벡터 저장소</b>: 인덱스 이름 및 기타 설정</li>
 * </ul>
 * 
 * <p><b>기본값:</b></p>
 * <ul>
 *   <li>documentsFolder: "documents"</li>
 *   <li>indexName: "rag-index"</li>
 *   <li>keyPrefix: "rag:"</li>
 *   <li>embeddingPrefix: "rag:embedding:"</li>
 * </ul>
 * 
 * <p><b>사용처:</b></p>
 * <ul>
 *   <li><b>VectorStoreManagementService</b>: 벡터 저장소 관리</li>
 *   <li><b>DocumentProcessingService</b>: 문서 처리</li>
 *   <li><b>FileManager</b>: 파일 시스템 작업</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "rag.vectorstore")
public class VectorStoreConfig {
    
    /** 문서 폴더 경로 (VectorStoreManagementService용) */
    private String documentsFolder = CommonConstants.DOCUMENTS_FOLDER_NAME;
    
    /** 문서 처리 폴더 경로 (DocumentProcessingService용) */
    private String processingDocumentsFolder = CommonConstants.DOCUMENTS_FOLDER_NAME;
    
    /** 벡터 인덱스 이름 */
    private String indexName = ConfigConstants.VECTOR_INDEX_NAME;
    
    /** Redis 키 접두사 */
    private String keyPrefix = ConfigConstants.REDIS_KEY_PREFIX;
    
    /** 임베딩 키 접두사 */
    private String embeddingPrefix = ConfigConstants.REDIS_EMBEDDING_KEY_PREFIX;
    
    // ==================== Getter/Setter ====================
    
    public String getDocumentsFolder() {
        return documentsFolder;
    }
    
    public void setDocumentsFolder(String documentsFolder) {
        this.documentsFolder = documentsFolder;
    }
    
    public String getProcessingDocumentsFolder() {
        return processingDocumentsFolder;
    }
    
    public void setProcessingDocumentsFolder(String processingDocumentsFolder) {
        this.processingDocumentsFolder = processingDocumentsFolder;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
    
    public String getEmbeddingPrefix() {
        return embeddingPrefix;
    }
    
    public void setEmbeddingPrefix(String embeddingPrefix) {
        this.embeddingPrefix = embeddingPrefix;
    }
    
    // ==================== 유틸리티 메서드 ====================
    
    /**
     * Redis 키 패턴 목록 생성
     * @return 키 패턴 리스트
     */
    public java.util.List<String> getKeyPatterns() {
        return java.util.List.of(keyPrefix + ConfigConstants.WILDCARD, embeddingPrefix + ConfigConstants.WILDCARD);
    }
    
    /**
     * 전체 키 패턴 문자열 생성
     * @return 전체 키 패턴
     */
    public String getFullKeyPattern() {
        return keyPrefix + ConfigConstants.WILDCARD;
    }
    
    /**
     * 임베딩 키 패턴 문자열 생성
     * @return 임베딩 키 패턴
     */
    public String getEmbeddingKeyPattern() {
        return embeddingPrefix + ConfigConstants.WILDCARD;
    }
    
    /**
     * 설정 정보를 맵으로 반환
     * @return 설정 정보 맵
     */
    public java.util.Map<String, Object> getConfigMap() {
        return java.util.Map.of(
            ConfigConstants.MAP_KEY_DOCUMENTS_FOLDER, documentsFolder,
            ConfigConstants.MAP_KEY_PROCESSING_DOCUMENTS_FOLDER, processingDocumentsFolder,
            ConfigConstants.MAP_KEY_INDEX_NAME, indexName,
            ConfigConstants.MAP_KEY_KEY_PREFIX, keyPrefix,
            ConfigConstants.MAP_KEY_EMBEDDING_PREFIX, embeddingPrefix
        );
    }
    
    @Override
    public String toString() {
        return String.format(ConfigConstants.VECTORSTORE_CONFIG_FORMAT,
                documentsFolder, processingDocumentsFolder, indexName, keyPrefix, embeddingPrefix);
    }
}
