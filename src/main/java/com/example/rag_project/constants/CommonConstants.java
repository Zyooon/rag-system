package com.example.rag_project.constants;

/**
 * 공통 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 여러 클래스에 걸쳐 중복으로 사용되는 모든 공통 상수를 관리합니다:</p>
 * <ul>
 *   <li>파일 확장자</li>
 *   <li>문서 폴더 관련</li>
 *   <li>시스템 속성</li>
 *   <li>메타데이터 키</li>
 *   <li>기타 공통 상수</li>
 * </ul>
 */
public final class CommonConstants {
    
    // ==================== 파일 확장자 ====================
    
    /** 텍스트 파일 확장자 */
    public static final String TXT_EXTENSION = ".txt";
    
    /** 마크다운 파일 확장자 */
    public static final String MD_EXTENSION = ".md";
    
    // ==================== 문서 폴더 관련 ====================
    
    /** 문서 폴더명 */
    public static final String DOCUMENTS_FOLDER_NAME = "documents";
    
    // ==================== 시스템 속성 ====================
    
    /** 시스템 사용자 디렉토리 속성 */
    public static final String SYSTEM_USER_DIR = "user.dir";
    
    // ==================== 메타데이터 키 ====================
    
    /** 파일명 메타데이터 키 */
    public static final String METADATA_KEY_FILENAME = "filename";
    
    /** 파일경로 메타데이터 키 */
    public static final String METADATA_KEY_FILEPATH = "filepath";
    
    /** 내용 키 */
    public static final String KEY_CONTENT = "content";
    
    /** 메타데이터 키 */
    public static final String KEY_METADATA = "metadata";
    
    /** 오류 키 */
    public static final String KEY_ERROR = "error";
    
    // ==================== README 관련 ====================
    
    /** README 파일명 */
    public static final String README_FILENAME = "README.md";
    
    // ==================== 기타 공통 상수 ====================
    
    /** 빈 문자열 */
    public static final String EMPTY_STRING = "";
    
    /** 줄바꿈 문자 */
    public static final String NEWLINE = "\n";
    
    /** 공백 문자 */
    public static final String SPACE = " ";
    
    // ==================== 파서 관련 상수 ====================
    
    /** Hierarchical 파서 이름 */
    public static final String PARSER_HIERARCHICAL = "Hierarchical";
    
    /** Bullet 파서 이름 */
    public static final String PARSER_BULLET = "Bullet";
    
    /** SimpleLine 파서 이름 */
    public static final String PARSER_SIMPLE_LINE = "SimpleLine";
    
    /** 알 수 없는 파서 에러 메시지 */
    public static final String ERROR_UNKNOWN_PARSER = "알 수 없는 파서: ";
    
    // ==================== 정규식 패턴 ====================
    
    /** Hierarchical 패턴 정규식 */
    public static final String PATTERN_HIERARCHICAL = "^(#{1,3}\\\\s+|\\\\d+\\\\.\\\\s+|\\\\[.*\\\\]|제목:)";
    
    /** Bullet 패턴 정규식 */
    public static final String PATTERN_BULLET = "^(\\\\d+\\\\.\\\\s+|[-*]\\\\s+|•\\\\s+)";
    
    // ==================== Map 키 상수 ====================
    
    /** 존재 여부 키 */
    public static final String KEY_EXISTS = "exists";
    
    /** 경로 키 */
    public static final String KEY_PATH = "path";
    
    /** 파일 수 키 */
    public static final String KEY_FILE_COUNT = "file_count";
    
    /** 파일 목록 키 */
    public static final String KEY_FILES = "files";
    
    /** 저장 시간 키 */
    public static final String KEY_SAVED_AT = "saved_at";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private CommonConstants() {
        throw new AssertionError("CommonConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
