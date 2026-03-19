package com.example.rag_project.splitter;

import java.util.List;

/**
 * TextSplitter 설정값 관리 클래스
 * 
 * <p>이 클래스는 텍스트 분할기의 모든 설정 상수를 관리합니다:</p>
 * <ul>
 *   <li><b>분할 크기</b> - 청크의 최대/최소 크기 설정</li>
 *   <li><b>임베딩 기준</b> - 벡터화를 위한 최소 길이 기준</li>
 *   <li><b>수량 제한</b> - 생성할 청크의 최대 수량</li>
 *   <li><b>구분자</b> - 문장 분리를 위한 구분자 문자들</li>
 * </ul>
 * 
 * <p><b>설정 카테고리:</b></p>
 * <ul>
 *   <li><b>기본 설정</b>: 일반 문서 처리용 표준 설정</li>
 *   <li><b>정밀 검색</b>: 더 세분화된 청크를 위한 설정</li>
 *   <li><b>속도 최적화</b>: 더 큰 청크를 위한 설정</li>
 * </ul>
 */
public class TextSplitterConfig {
    
    // ==================== 기본 설정 ====================
    
    /** 기본 청크 크기 (토큰 단위) */
    public static final int DEFAULT_CHUNK_SIZE = 300;
    
    /** 최소 청크 문자 수 */
    public static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 50;
    
    /** 임베딩 최소 길이 */
    public static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 20;
    
    /** 최대 청크 수 */
    public static final int DEFAULT_MAX_NUM_CHUNKS = 500;
    
    /** 구분자 유지 여부 */
    public static final boolean DEFAULT_KEEP_SEPARATOR = true;
    
    /** 기본 구분자 문자들 */
    public static final List<Character> DEFAULT_PUNCTUATION_MARKS = List.of('.', '\n', ']', '-', '|');
    
    // ==================== 정밀 검색 설정 ====================
    
    /** 정밀 검색용 청크 크기 (더 세분화) */
    public static final int PRECISE_CHUNK_SIZE = 100;
    
    /** 정밀 검색용 최소 청크 문자 수 (더 짧음) */
    public static final int PRECISE_MIN_CHUNK_SIZE_CHARS = 30;
    
    /** 정밀 검색용 임베딩 최소 길이 (더 낮음) */
    public static final int PRECISE_MIN_CHUNK_LENGTH_TO_EMBED = 10;
    
    /** 정밀 검색용 최대 청크 수 (더 많음) */
    public static final int PRECISE_MAX_NUM_CHUNKS = 800;
    
    // ==================== 속도 최적화 설정 ====================
    
    /** 속도 최적화용 청크 크기 (더 큼) */
    public static final int SPEED_CHUNK_SIZE = 400;
    
    /** 속도 최적화용 최소 청크 문자 수 (더 김) */
    public static final int SPEED_MIN_CHUNK_SIZE_CHARS = 100;
    
    /** 속도 최적화용 임베딩 최소 길이 */
    public static final int SPEED_MIN_CHUNK_LENGTH_TO_EMBED = 10;
    
    /** 속도 최적화용 최대 청크 수 (더 많음) */
    public static final int SPEED_MAX_NUM_CHUNKS = 1000;
    
    // ==================== 문서 길이 기준 ====================
    
    /** 긴 문서 기준 길이 (800자 이상이면 분할 대상) */
    public static final int LONG_DOCUMENT_THRESHOLD = 800;
    
    // ==================== 설정 조합 ====================
    
    /**
     * 기본 설정 조합을 반환
     * @return 기본 설정 맵
     */
    public static SplitterSettings getDefaultSettings() {
        return new SplitterSettings(
            DEFAULT_CHUNK_SIZE,
            DEFAULT_MIN_CHUNK_SIZE_CHARS,
            DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED,
            DEFAULT_MAX_NUM_CHUNKS,
            DEFAULT_KEEP_SEPARATOR,
            DEFAULT_PUNCTUATION_MARKS
        );
    }
    
    /**
     * 정밀 검색 설정 조합을 반환
     * @return 정밀 검색 설정 맵
     */
    public static SplitterSettings getPreciseSearchSettings() {
        return new SplitterSettings(
            PRECISE_CHUNK_SIZE,
            PRECISE_MIN_CHUNK_SIZE_CHARS,
            PRECISE_MIN_CHUNK_LENGTH_TO_EMBED,
            PRECISE_MAX_NUM_CHUNKS,
            DEFAULT_KEEP_SEPARATOR,
            DEFAULT_PUNCTUATION_MARKS
        );
    }
    
    /**
     * 속도 최적화 설정 조합을 반환
     * @return 속도 최적화 설정 맵
     */
    public static SplitterSettings getSpeedOptimizationSettings() {
        return new SplitterSettings(
            SPEED_CHUNK_SIZE,
            SPEED_MIN_CHUNK_SIZE_CHARS,
            SPEED_MIN_CHUNK_LENGTH_TO_EMBED,
            SPEED_MAX_NUM_CHUNKS,
            DEFAULT_KEEP_SEPARATOR,
            DEFAULT_PUNCTUATION_MARKS
        );
    }
    
    /**
     * 분할기 설정을 담는 내부 클래스
     */
    public static class SplitterSettings {
        private final int chunkSize;
        private final int minChunkSizeChars;
        private final int minChunkLengthToEmbed;
        private final int maxNumChunks;
        private final boolean keepSeparator;
        private final List<Character> punctuationMarks;
        
        public SplitterSettings(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
                              int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
            this.chunkSize = chunkSize;
            this.minChunkSizeChars = minChunkSizeChars;
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            this.maxNumChunks = maxNumChunks;
            this.keepSeparator = keepSeparator;
            this.punctuationMarks = punctuationMarks;
        }
        
        // Getter 메서드들
        public int getChunkSize() { return chunkSize; }
        public int getMinChunkSizeChars() { return minChunkSizeChars; }
        public int getMinChunkLengthToEmbed() { return minChunkLengthToEmbed; }
        public int getMaxNumChunks() { return maxNumChunks; }
        public boolean isKeepSeparator() { return keepSeparator; }
        public List<Character> getPunctuationMarks() { return punctuationMarks; }
        
        @Override
        public String toString() {
            return String.format("SplitterSettings{chunkSize=%d, minChars=%d, minEmbed=%d, maxChunks=%d, keepSep=%s}",
                    chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator);
        }
    }
}
