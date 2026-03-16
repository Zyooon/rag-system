package com.example.rag_project.constants;

/**
 * 메시지 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 사용자 메시지 관련 모든 상수를 관리합니다:</p>
 * <ul>
 *   <li>사용자 응답 메시지</li>
 *   <li>시스템 상태 메시지</li>
 * </ul>
 */
public final class MessageConstants {
    
    // ==================== 사용자 응답 메시지 ====================
    
    /** 관련 정보 없음 메시지 */
    public static final String MSG_NO_RELEVANT_INFO = "관련 정보를 찾을 수 없습니다.";
    
    /** 신뢰할 수 있는 정보 없음 메시지 */
    public static final String MSG_NO_RELIABLE_INFO = "질문과 관련된 충분히 신뢰할 수 있는 정보를 찾을 수 없습니다.";
    
    /** README 파일명 */
    public static final String README_FILENAME = "README.md";
    
    /** 지식 베이스 데이터 없음 메시지 */
    public static final String MSG_NO_KNOWLEDGE_BASE = "현재 지식 베이스(Redis)에 저장된 데이터가 없어 답변을 드릴 수 없습니다.";
    
    /** 상태 메시지 상수 */
    public static final String MSG_DOCUMENTS_LOADED = "문서가 로드되어 있습니다. (벡터 저장소 기준)";
    public static final String MSG_DOCUMENTS_NOT_LOADED = "문서가 로드되지 않았습니다.";
    public static final String MSG_VECTORSTORE_MARKED_INITIALIZED = "벡터 저장소가 초기화되었음으로 표시됨";
    public static final String MSG_FILE_LOAD_SUCCESS = "파일에서 Redis로 문서 로드가 완료되었습니다.";
    public static final String MSG_FILE_LOAD_FAILED_PREFIX = "로드 실패: ";
    public static final String MSG_ERROR_PREFIX = "오류 발생: ";
    
    /** 메시지 상수 */
    public static final String MSG_FOLDER_NOT_EXISTS = "폴더가 존재하지 않습니다.";
    public static final String MSG_NO_TEXT_FILES = "폴더에 텍스트 파일이 없습니다.";
    public static final String MSG_FILES_PROCESSED = "총 %d개 파일 처리 완료: %d개 저장, %d개 중복";
    
    /** 데이터 로드 요청 메시지 */
    public static final String LOG_MANUAL_LOAD_REQUEST = "수동으로 데이터를 로드해주세요 (/load-from-files API 호출).";
    
    /** VectorStore 데이터 정리 완료 메시지 */
    public static final String MSG_VECTORSTORE_DATA_CLEANED = "VectorStore 데이터 정리 완료";
    
    /** VectorStore 데이터 정리 중 오류 메시지 */
    public static final String MSG_VECTORSTORE_CLEAN_ERROR = "VectorStore 데이터 정리 중 오류: {}";
    
    /** Redis 키 삭제 중 오류 메시지 */
    public static final String MSG_REDIS_KEY_DELETE_ERROR = "Redis 키 삭제 중 오류: {}";
    
    /** 벡터 저장소 초기화 중 오류 메시지 */
    public static final String MSG_VECTORSTORE_INIT_ERROR = "벡터 저장소 초기화 중 오류 발생";
    
    /** Redis 로드 관련 상수 */
    public static final String MSG_REDIS_NO_DOCUMENTS = "Redis에 저장된 문서가 없습니다.";
    public static final String MSG_REDIS_LOAD_SUCCESS = "Redis에서 {}개 문서를 벡터 저장소에 로드했습니다.";
    public static final String MSG_REDIS_LOAD_FAILED = "Redis 문서 로드 실패: {}";
    public static final String MSG_REDIS_LOAD_ERROR = "Redis 문서 로드 중 오류 발생";
    
    /** 검색 관련 상수 */
    public static final String MSG_NO_RELEVANT_INFO_FOUND = "관련 정보를 찾을 수 없습니다.";
    public static final String MSG_AI_ANSWER_ERROR = "AI 답변 생성 중 오류: ";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private MessageConstants() {
        throw new AssertionError("MessageConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
