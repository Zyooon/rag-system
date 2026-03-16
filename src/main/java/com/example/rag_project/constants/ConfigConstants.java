package com.example.rag_project.constants;

/**
 * 설정 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 설정 관련 모든 상수를 관리합니다:</p>
 * <ul>
 *   <li>설정 키</li>
 *   <li>기본값</li>
 *   <li>시스템 속성</li>
 * </ul>
 */
public final class ConfigConstants {
    
    // ==================== 설정 키 ====================
    
    /** 문서 폴더 설정 키 */
    public static final String CONFIG_DOCUMENTS_FOLDER = "rag.documents.folder";
    
    /** 벡터 저장소 타입 설정 키 */
    public static final String CONFIG_VECTORSTORE_TYPE = "rag.vectorstore.type";
    
    /** 벡터 인덱스 이름 설정 키 */
    public static final String CONFIG_VECTORSTORE_INDEX_NAME = "rag.redis.vectorstore.index-name";
    
    /** 벡터 키 접두사 설정 키 */
    public static final String CONFIG_VECTORSTORE_KEY_PREFIX = "rag.redis.vectorstore.key-prefix";
    
    /** 임베딩 키 접두사 설정 키 */
    public static final String CONFIG_VECTORSTORE_EMBEDDING_PREFIX = "rag.redis.vectorstore.embedding-prefix";
    
    /** 검색 임계값 설정 키 */
    public static final String CONFIG_SEARCH_THRESHOLD = "rag.search.threshold";
    
    /** 최대 검색 결과 설정 키 */
    public static final String CONFIG_SEARCH_MAX_RESULTS = "rag.search.max-results";
    
    // ==================== 기본값 ====================
    
    /** 기본 문서 폴더 */
    public static final String DEFAULT_DOCUMENTS_FOLDER = "documents";
    
    /** 기본 벡터 저장소 타입 */
    public static final String DEFAULT_VECTORSTORE_TYPE = "redis";
    
    /** 기본 검색 임계값 */
    public static final double DEFAULT_SEARCH_THRESHOLD = 0.3;
    
    /** 기본 최대 검색 결과 수 */
    public static final int DEFAULT_MAX_RESULTS = 5;
    
    /** 기본 Ollama 채팅 모델 */
    public static final String DEFAULT_OLLAMA_CHAT_MODEL = "llama3";
    
    /** 기본 Ollama 임베딩 모델 */
    public static final String DEFAULT_OLLAMA_EMBEDDING_MODEL = "bge-m3";
    
    /** 기본 Ollama 기본 URL */
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    
    // ==================== 시스템 속성 ====================
    
    /** 시스템 속성 상수 */
    public static final String SYSTEM_USER_DIR = "user.dir";
    
    /** 문서 폴더명 상수 */
    public static final String DOCUMENTS_FOLDER_NAME = "documents";
    
    /** 리소스 경로 상수 */
    public static final String RESOURCE_CLASSPATH_PREFIX = "classpath:";
    
    /** 기본 문자 집합 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /** 기본 서버 포트 */
    public static final int DEFAULT_SERVER_PORT = 8080;
    
    /** 파일 확장자 상수 */
    public static final String TXT_EXTENSION = ".txt";
    public static final String MD_EXTENSION = ".md";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private ConfigConstants() {
        throw new AssertionError("ConfigConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
