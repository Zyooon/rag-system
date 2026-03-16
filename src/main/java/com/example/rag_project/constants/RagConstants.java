package com.example.rag_project.constants;

/**
 * RAG 시스템 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 사용되는 모든 상수 문자열을 중앙 관리합니다:</p>
 * <ul>
 *   <li>Redis 키 접두사 및 패턴</li>
 *   <li>메타데이터 필드명</li>
 *   <li>설정 기본값</li>
 *   <li>에러 메시지</li>
 * </ul>
 * 
 * <p><b>장점:</b></p>
 * <ul>
 *   <li>문자열 오타 방지</li>
 *   <li>일관된 명명 규칙</li>
 *   <li>유지보수 용이성</li>
 *   <li>코드 재사용성</li>
 * </ul>
 */
public final class RagConstants {
    
    // ==================== Redis 관련 상수 ====================
    
    /** Redis 키 접두사 */
    public static final String REDIS_KEY_PREFIX = "rag:";
    
    /** Redis 문서 키 접두사 */
    public static final String REDIS_DOCUMENT_KEY_PREFIX = "rag:document:";
    
    /** Redis 임베딩 키 접두사 */
    public static final String REDIS_EMBEDDING_KEY_PREFIX = "embedding:";
    
    /** 벡터 인덱스 이름 */
    public static final String VECTOR_INDEX_NAME = "vector_index";
    
    // ==================== 메타데이터 필드명 ====================
    
    /** 파일명 메타데이터 필드 */
    public static final String METADATA_FILENAME = "filename";
    
    /** 파일경로 메타데이터 필드 */
    public static final String METADATA_FILEPATH = "filepath";
    
    /** 청크 ID 메타데이터 필드 */
    public static final String METADATA_CHUNK_ID = "chunk_id";
    
    /** 저장 시간 메타데이터 필드 */
    public static final String METADATA_SAVED_AT = "saved_at";
    
    /** 거리 메타데이터 필드 */
    public static final String METADATA_DISTANCE = "distance";
    
    /** 벡터 점수 메타데이터 필드 */
    public static final String METADATA_VECTOR_SCORE = "vector_score";
    
    // ==================== 설정 기본값 ====================
    
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
    
    // ==================== 에러 메시지 ====================
    
    /** 문서 로드 실패 에러 */
    public static final String ERROR_DOCUMENT_LOAD_FAILED = "문서 로드 실패: ";
    
    /** 벡터 저장소 초기화 실패 에러 */
    public static final String ERROR_VECTORSTORE_INIT_FAILED = "벡터 저장소 초기화 실패: ";
    
    /** Redis 문서 로드 실패 에러 */
    public static final String ERROR_REDIS_LOAD_FAILED = "Redis 문서 로드 실패: ";
    
    /** 검색 실패 에러 */
    public static final String ERROR_SEARCH_FAILED = "검색 실패: ";
    
    /** 잘못된 인자 에러 */
    public static final String ERROR_INVALID_ARGUMENT = "잘못된 인자: ";
    
    // ==================== 로그 메시지 ====================
    
    /** 벡터 저장소 초기화 완료 로그 */
    public static final String LOG_VECTORSTORE_INITIALIZED = "벡터 저장소가 초기화되었습니다.";
    
    /** 문서 로드 완료 로그 포맷 */
    public static final String LOG_DOCUMENTS_LOADED = "문서 로드 완료: {}, 시스템 초기화됨";
    
    /** Redis 문서 로드 완료 로그 포맷 */
    public static final String LOG_REDIS_DOCUMENTS_LOADED = "Redis에서 {}개 문서를 벡터 저장소에 로드했습니다.";
    
    /** Redis 키 삭제 완료 로그 포맷 */
    public static final String LOG_REDIS_KEYS_DELETED = "RAG 관련 키 {}개 삭제 완료";
    
    /** Embedding 키 삭제 완료 로그 포맷 */
    public static final String LOG_EMBEDDING_KEYS_DELETED = "Embedding 관련 키 {}개 삭제 완료";
    
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
    
    // ==================== 기타 상수 ====================
    
    /** 알 수 없는 파일명 */
    public static final String UNKNOWN_FILENAME = "unknown.txt";
    
    /** 알 수 없음 */
    public static final String UNKNOWN = "Unknown";
    
    /** 기본 문자 집합 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /** 기본 서버 포트 */
    public static final int DEFAULT_SERVER_PORT = 8080;
    
    // ==================== private 생성자 ====================
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private RagConstants() {
        throw new AssertionError("RagConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
