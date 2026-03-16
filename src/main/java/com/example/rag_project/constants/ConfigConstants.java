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
    
    // ==================== 파서 관련 상수 ====================
    
    /** 기본 부모 제목 */
    public static final String DEFAULT_PARENT_TITLE = "일반 항목";
    
    /** 불릿 섹션 타입 */
    public static final String SECTION_TYPE_BULLET = "bullet";
    
    /** 메타데이터 키 */
    public static final String METADATA_KEY_TITLE = "title";
    public static final String METADATA_KEY_SECTION_TYPE = "section_type";
    public static final String METADATA_KEY_START_LINE = "start_line";
    public static final String METADATA_KEY_FILENAME = "filename";
    
    /** 제목 길이 제한 */
    public static final int MAX_TITLE_LENGTH = 50;
    public static final int TRUNCATED_TITLE_LENGTH = 47;
    
    /** 콜론 제한 길이 */
    public static final int COLON_LIMIT_LENGTH = 30;
    
    /** 표 최소 컬럼 수 */
    public static final int MIN_TABLE_COLUMNS = 3;
    
    /** 생략 문자열 */
    public static final String ELLIPSIS = "...";
    
    /** 줄바꿈 문자 */
    public static final String NEWLINE = "\n";
    
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
    
    /** 빈 문자열 */
    public static final String EMPTY_STRING = "";
    
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
    public static final String SPACE_SEPARATOR = " ";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private ConfigConstants() {
        throw new AssertionError("ConfigConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
