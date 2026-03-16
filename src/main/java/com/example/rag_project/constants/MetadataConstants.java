package com.example.rag_project.constants;

/**
 * 메타데이터 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 메타데이터 관련 모든 상수를 관리합니다:</p>
 * <ul>
 *   <li>문서 메타데이터 필드명</li>
 *   <li>JSON 키 상수</li>
 *   <li>Map 키 상수</li>
 * </ul>
 */
public final class MetadataConstants {
    
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
    
    /** 메타데이터 키 상수 */
    public static final String METADATA_FILE_CHUNK_INDEX = "file_chunk_index";
    
    // ==================== JSON 키 상수 ====================
    
    /** 메타데이터 JSON 키 */
    public static final String JSON_KEY_METADATA = "metadata";
    
    /** 파일명 JSON 키 */
    public static final String JSON_KEY_FILENAME = "filename";
    
    /** 내용 JSON 키 */
    public static final String JSON_KEY_CONTENT = "content";
    
    // ==================== Map 키 상수 ====================
    
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
    
    // ==================== 기타 상수 ====================
    
    /** 알 수 없는 파일명 */
    public static final String UNKNOWN_FILENAME = "unknown.txt";
    
    /** 알 수 없음 */
    public static final String UNKNOWN = "Unknown";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private MetadataConstants() {
        throw new AssertionError("MetadataConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
