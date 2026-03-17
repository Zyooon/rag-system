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
    
    /** 기본 문서 폴더 - CommonConstants에서 관리 */
    @Deprecated
    public static final String DEFAULT_DOCUMENTS_FOLDER = CommonConstants.DOCUMENTS_FOLDER_NAME;
    
    // ==================== Redis 관련 상수 ====================
    
    /** Redis 키 접두사 */
    public static final String REDIS_KEY_PREFIX = "rag:";
    
    /** Redis 문서 키 접두사 */
    public static final String REDIS_DOCUMENT_KEY_PREFIX = "rag:document:";
    
    /** Redis 임베딩 키 접두사 */
    public static final String REDIS_EMBEDDING_KEY_PREFIX = "embedding:";
    
    /** 벡터 인덱스 이름 */
    public static final String VECTOR_INDEX_NAME = "vector_index";
    
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
    
    /** 기본 Redis 호스트 */
    public static final String DEFAULT_REDIS_HOST = "localhost";
    
    /** 기본 Redis 포트 */
    public static final int DEFAULT_REDIS_PORT = 6379;
    
    /** 기본 Redis 데이터베이스 */
    public static final int DEFAULT_REDIS_DATABASE = 0;
    
    /** 벡터 저장소 접두사 */
    public static final String VECTORSTORE_PREFIX = "rag:";
    
    /** 벡터 인덱스 이름 */
    public static final String VECTORSTORE_INDEX_NAME = "vector_index";
    
    /** 와일드카드 문자 */
    public static final String WILDCARD = "*";
    
    /** 설정 맵 키 상수 */
    public static final String MAP_KEY_DOCUMENTS_FOLDER = "documentsFolder";
    public static final String MAP_KEY_PROCESSING_DOCUMENTS_FOLDER = "processingDocumentsFolder";
    public static final String MAP_KEY_INDEX_NAME = "indexName";
    public static final String MAP_KEY_KEY_PREFIX = "keyPrefix";
    public static final String MAP_KEY_EMBEDDING_PREFIX = "embeddingPrefix";
    
    /** toString 포맷 */
    public static final String VECTORSTORE_CONFIG_FORMAT = "VectorStoreConfig{documentsFolder='%s', processingDocumentsFolder='%s', indexName='%s', keyPrefix='%s', embeddingPrefix='%s'}";
    
    // ==================== 컨트롤러 관련 상수 ====================
    
    /** Redis 연결 상태 */
    public static final String REDIS_CONNECTION_CONNECTED = "connected";
    
    /** 벡터 저장소 타입 */
    public static final String VECTORSTORE_TYPE_SIMPLE_REDIS_BACKUP = "simple_with_redis_backup";
    
    /** 성공 메시지 */
    public static final String MSG_REDIS_CONNECTION_CHECK = "Redis 연결 상태 확인";
    public static final String MSG_REDIS_STATUS_CHECK_FAILED = "Redis 상태 확인 실패: ";
    public static final String MSG_REDIS_VECTORSTORE_DELETE_COMPLETE = "Redis Vector Store 삭제 완료 - 총 %d개 파일 삭제 (RAG: %d개, Embedding: %d개)";
    public static final String MSG_REDIS_VECTORSTORE_DELETE_FAILED = "Redis Vector Store 삭제 실패: ";
    public static final String MSG_REDIS_VECTORSTORE_BUILD_FAILED = "Redis Vector Store 구축 실패: ";
    public static final String MSG_DOCUMENTS_RELOADED = "문서가 다시 로드되었습니다. (%d개 파일 삭제 후 재로드)";
    public static final String MSG_DOCUMENT_RELOAD_FAILED = "문서 재로드 실패: ";
    
    /** Map 키 */
    public static final String MAP_KEY_REDIS_CONNECTION = "redis_connection";
    public static final String MAP_KEY_VECTOR_STORE_TYPE = "vector_store_type";
    
    // ==================== SourceInfo 관련 상수 ====================
    
    /** 제목 형식 접두사 */
    public static final String TITLE_PREFIX_BRACKET_START = "[";
    public static final String TITLE_PREFIX_BRACKET_END = "]";
    public static final String TITLE_PREFIX_MARKDOWN = "# ";
    public static final String TITLE_PREFIX_KOREAN = "제목:";
    
    /** 파일 확장자 */
    public static final String TXT_EXTENSION = ".txt";
    public static final String MD_EXTENSION = ".md";
    
    /** DecimalFormat 패턴 */
    public static final String SCORE_DECIMAL_FORMAT = "#.##";
    
    /** 메타데이터 키 */
    public static final String METADATA_KEY_CHUNK_ID = "chunk_id";
    
    /** 리소스 경로 상수 */
    public static final String RESOURCE_CLASSPATH_PREFIX = "classpath:";
    
    /** 기본 문자 집합 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /** 기본 서버 포트 */
    public static final int DEFAULT_SERVER_PORT = 8080;
    
    // ==================== 파서 관련 상수 ====================
    
    /** 기본 부모 제목 */
    public static final String DEFAULT_PARENT_TITLE = "일반 항목";
    
    /** 불릿 섹션 타입 */
    public static final String SECTION_TYPE_BULLET = "bullet";
    
    /** 메타데이터 키 */
    public static final String METADATA_KEY_TITLE = "title";
    public static final String METADATA_KEY_SECTION_TYPE = "section_type";
    public static final String METADATA_KEY_START_LINE = "start_line";
    /** 메타데이터 키 - CommonConstants에서 관리 */
    @Deprecated
    public static final String METADATA_KEY_FILENAME = CommonConstants.METADATA_KEY_FILENAME;
    
    // ==================== 메타데이터 관련 상수 ====================
    
    /** 청크 ID 메타데이터 필드 */
    public static final String METADATA_CHUNK_ID = "chunk_id";
    
    /** 저장 시간 메타데이터 필드 */
    public static final String METADATA_SAVED_AT = "saved_at";
    
    /** 거리 메타데이터 필드 */
    public static final String METADATA_DISTANCE = "distance";
    
    /** 벡터 점수 메타데이터 필드 */
    public static final String METADATA_VECTOR_SCORE = "vector_score";
    
    /** 메타데이터 키 상수 */
    public static final String METADATA_FILE_CHUNK_INDEX = "file_chunk_index";
    
    /** 메타데이터 JSON 키 */
    public static final String JSON_KEY_METADATA = "metadata";
    
    /** 성공 여부 Map 키 */
    public static final String MAP_KEY_SUCCESS = "success";
    
    /** 메시지 Map 키 */
    public static final String MAP_KEY_MESSAGE = "message";
    
    /** 문서 개수 Map 키 */
    public static final String MAP_KEY_DOCUMENT_COUNT = "documentCount";
    
    /** 로드된 파일 목록 Map 키 */
    public static final String MAP_KEY_LOADED_FILES = "loadedFiles";
    
    /** 초기화 여부 Map 키 */
    public static final String MAP_KEY_IS_INITIALIZED = "isInitialized";
    
    /** 저장 개수 Map 키 */
    public static final String MAP_KEY_SAVED_COUNT = "savedCount";
    
    /** 중복 개수 Map 키 */
    public static final String MAP_KEY_DUPLICATE_COUNT = "duplicateCount";
    
    /** 전체 개수 Map 키 */
    public static final String MAP_KEY_TOTAL_COUNT = "totalCount";
    
    /** 원본 파일 개수 Map 키 */
    public static final String MAP_KEY_ORIGINAL_FILE_COUNT = "originalFileCount";
    
    /** 답변 Map 키 */
    public static final String MAP_KEY_ANSWER = "answer";
    
    /** 출처 Map 키 */
    public static final String MAP_KEY_SOURCES = "sources";
    
    /** 전체 삭제 개수 Map 키 */
    public static final String MAP_KEY_TOTAL_DELETED = "totalDeleted";
    
    /** RAG 키 삭제 개수 Map 키 */
    public static final String MAP_KEY_RAG_KEYS_DELETED = "ragKeysDeleted";
    
    /** 임베딩 키 삭제 개수 Map 키 */
    public static final String MAP_KEY_EMBEDDING_KEYS_DELETED = "embeddingKeysDeleted";
    
    /** 오류 Map 키 - CommonConstants에서 관리 */
    @Deprecated
    public static final String MAP_KEY_ERROR = CommonConstants.KEY_ERROR;
    
    /** RAG 키 Map 키 */
    public static final String MAP_KEY_RAG_KEYS = "ragKeys";
    
    /** 임베딩 키 Map 키 */
    public static final String MAP_KEY_EMBEDDING_KEYS = "embeddingKeys";
    
    /** 알 수 없는 파일명 */
    public static final String UNKNOWN_FILENAME = "unknown.txt";
    
    /** 알 수 없음 */
    public static final String UNKNOWN = "Unknown";
    
    /** 제목 길이 제한 */
    public static final int MAX_TITLE_LENGTH = 50;
    public static final int TRUNCATED_TITLE_LENGTH = 47;
    
    /** 콜론 제한 길이 */
    public static final int COLON_LIMIT_LENGTH = 30;
    
    /** 표 최소 컬럼 수 */
    public static final int MIN_TABLE_COLUMNS = 3;
    
    /** 생략 문자열 */
    public static final String ELLIPSIS = "...";
    
    /** 줄바꿈 문자 - CommonConstants에서 관리 */
    // 위에서 이미 처리됨
    
    /** 포맷 문자열 */
    public static final String SECTION_FORMAT = "[%s]\n%s";
    
    /** 계층적 파서 관련 상수 */
    public static final String SECTION_TYPE_BULLET_WITH_HEADER = "bullet_with_header";
    public static final String METADATA_KEY_HEADER = "header";
    public static final String METADATA_KEY_HEADER_LINE = "header_line";
    public static final String METADATA_KEY_BODY_SUMMARY = "body_summary";
    public static final String METADATA_KEY_H1 = "h1";
    public static final String METADATA_KEY_H2 = "h2";
    public static final String METADATA_KEY_H3 = "h3";
    public static final String METADATA_KEY_HEADING_LEVEL = "heading_level";
    
    /** 제목 레벨 */
    public static final String HEADING_LEVEL_1 = "1";
    public static final String HEADING_LEVEL_2 = "2";
    public static final String HEADING_LEVEL_3 = "3";
    
    /** 길이 제한 상수 */
    public static final int MAX_HEADER_LENGTH = 60;
    public static final int MAX_BODY_SUMMARY_LENGTH = 100;
    public static final int TRUNCATED_BODY_SUMMARY_LENGTH = 97;
    
    /** 빈 문자열 - CommonConstants에서 관리 */
    @Deprecated
    public static final String EMPTY_STRING = CommonConstants.EMPTY_STRING;
    
    /** 줄바꿈 문자 - CommonConstants에서 관리 */
    @Deprecated
    public static final String NEWLINE = CommonConstants.NEWLINE;
    
    /** Markdown 파서 관련 상수 */
    public static final String SECTION_TYPE_MARKDOWN = "markdown";
    
    /** Markdown 제목 형식 */
    public static final String MARKDOWN_HEADING_PATTERN = "^#{1,6}\\s+.+";
    public static final String MARKDOWN_HEADING_PATTERN_1_3 = "^#{1,3}\\s+.+";
    
    /** Markdown 제목 접두사 */
    public static final String H1_PREFIX = "# ";
    public static final String H2_PREFIX = "## ";
    public static final String H3_PREFIX = "### ";
    
    /** 제목 분리 문자열 */
    public static final String TITLE_SEPARATOR = " > ";
    
    /** 계층 정보 포맷 */
    public static final String HIERARCHY_FORMAT = "[%s > %s > %s]\n%s";
    
    /** 최소 Markdown 제목 수 */
    public static final int MIN_MARKDOWN_HEADINGS = 2;
    
    /** SimpleLineParser 관련 상수 */
    public static final String SECTION_TYPE_PARAGRAPH = "paragraph";
    public static final String METADATA_KEY_PARAGRAPH_INDEX = "paragraph_index";
    public static final String METADATA_KEY_CHUNK_LENGTH = "chunk_length";
    
    /** 청크 길이 제한 */
    public static final int MAX_CHUNK_LENGTH = 500;
    public static final int MIN_CHUNK_LENGTH = 50;
    public static final int MIN_TITLE_LENGTH = 10;
    
    /** 문장 분리 정규식 */
    public static final String SENTENCE_SPLIT_REGEX = "(?<=[.!?])\\s+";
    public static final String TITLE_SENTENCE_SPLIT_REGEX = "[.!?]";
    
    /** 문단 분리 문자열 */
    public static final String PARAGRAPH_SEPARATOR = "\n\n";
    /** 공백 문자 - CommonConstants에서 관리 */
    @Deprecated
    public static final String SPACE_SEPARATOR = CommonConstants.SPACE;
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private ConfigConstants() {
        throw new AssertionError("ConfigConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
