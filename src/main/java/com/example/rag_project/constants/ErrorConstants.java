package com.example.rag_project.constants;

/**
 * 에러 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 에러 관련 모든 상수를 관리합니다:</p>
 * <ul>
 *   <li>에러 메시지</li>
 *   <li>에러 로그</li>
 * </ul>
 */
public final class ErrorConstants {
    
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
    
    // ==================== 에러 로그 상수 ====================
    
    /** 파일 로드 실패 에러 로그 */
    public static final String LOG_FILE_LOAD_FAILED = "파일에서 Redis로 데이터 로드 실패: {}";
    
    /** 예상치 못한 오류 에러 로그 */
    public static final String LOG_UNEXPECTED_ERROR = "예상치 못한 오류: {}";
    
    /** 메타데이터 JSON 파싱 실패 에러 로그 */
    public static final String LOG_METADATA_JSON_PARSE_FAILED = "메타데이터 JSON 파싱 실패: {}";
    
    /** 문서 처리 실패 에러 로그 */
    public static final String LOG_DOCUMENT_PROCESS_FAILED = "문서 처리 실패 ({}): {}";
    
    /** 문서 상태 확인 실패 에러 로그 */
    public static final String LOG_DOCUMENT_STATUS_CHECK_FAILED = "문서 상태 확인 실패: {}";
    
    /** Redis 벡터 저장소 상태 확인 실패 에러 로그 */
    public static final String LOG_REDIS_VECTORSTORE_STATUS_FAILED = "Redis 벡터 저장소 상태 확인 실패: {}";
    
    /** Redis 상태 확인 오류 에러 로그 */
    public static final String LOG_REDIS_STATUS_CHECK_ERROR = "Redis 상태 확인 중 오류: {}";
    
    // ==================== API 응답 키 상수 ====================
    
    /** 성공 여부 키 */
    public static final String RESPONSE_KEY_SUCCESS = "success";
    
    /** 타임스탬프 키 */
    public static final String RESPONSE_KEY_TIMESTAMP = "timestamp";
    
    /** 에러 타입 키 */
    public static final String RESPONSE_KEY_ERROR = "error";
    
    /** 메시지 키 */
    public static final String RESPONSE_KEY_MESSAGE = "message";
    
    /** 경로 키 */
    public static final String RESPONSE_KEY_PATH = "path";
    
    /** 상태 코드 키 */
    public static final String RESPONSE_KEY_STATUS = "status";
    
    // ==================== 에러 타입 상수 ====================
    
    /** 내부 서버 에러 */
    public static final String ERROR_TYPE_INTERNAL_SERVER = "Internal Server Error";
    
    /** 런타임 에러 */
    public static final String ERROR_TYPE_RUNTIME = "Runtime Error";
    
    /** RAG 서비스 에러 */
    public static final String ERROR_TYPE_RAG_SERVICE = "RAG Service Error";
    
    /** 잘못된 인자 에러 */
    public static final String ERROR_TYPE_INVALID_ARGUMENT = "Invalid Argument";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private ErrorConstants() {
        throw new AssertionError("ErrorConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
