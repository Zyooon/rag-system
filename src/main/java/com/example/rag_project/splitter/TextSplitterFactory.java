package com.example.rag_project.splitter;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

/**
 * TokenTextSplitter 생성을 위한 팩토리 클래스
 * 다양한 설정으로 TokenTextSplitter를 생성하고 관리
 */
public class TextSplitterFactory {
    
    // 기본 설정 상수
    public static final int DEFAULT_CHUNK_SIZE = 300;
    public static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 50;
    public static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 20;
    public static final int DEFAULT_MAX_NUM_CHUNKS = 500;
    public static final boolean DEFAULT_KEEP_SEPARATOR = true;
    public static final List<Character> DEFAULT_PUNCTUATION_MARKS = List.of('.', '\n', ']', '-');
    
    /**
     * 기본 설정으로 TokenTextSplitter 생성
     * @return 기본 설정이 적용된 TokenTextSplitter
     */
    public static TokenTextSplitter createDefault() {
        return new TokenTextSplitter(
            DEFAULT_CHUNK_SIZE,
            DEFAULT_MIN_CHUNK_SIZE_CHARS,
            DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED,
            DEFAULT_MAX_NUM_CHUNKS,
            DEFAULT_KEEP_SEPARATOR,
            DEFAULT_PUNCTUATION_MARKS
        );
    }
    
    /**
     * 사용자 정의 설정으로 TokenTextSplitter 생성
     * @param chunkSize 청크 크기
     * @param minChunkSizeChars 최소 청크 문자 수
     * @param minChunkLengthToEmbed 임베딩 최소 길이
     * @param maxNumChunks 최대 청크 수
     * @param keepSeparator 구분자 유지 여부
     * @param punctuationMarks 구분자 목록
     * @return 사용자 정의 설정이 적용된 TokenTextSplitter
     */
    public static TokenTextSplitter createCustom(
            int chunkSize, 
            int minChunkSizeChars, 
            int minChunkLengthToEmbed, 
            int maxNumChunks, 
            boolean keepSeparator, 
            List<Character> punctuationMarks) {
        return new TokenTextSplitter(
            chunkSize,
            minChunkSizeChars,
            minChunkLengthToEmbed,
            maxNumChunks,
            keepSeparator,
            punctuationMarks
        );
    }
    
    /**
     * 정밀 검색용 설정으로 TokenTextSplitter 생성
     * @return 정밀 검색용 TokenTextSplitter
     */
    public static TokenTextSplitter createForPreciseSearch() {
        return new TokenTextSplitter(
            100,    // chunkSize: 더 세분화된 청크
            30,     // minChunkSizeChars: 더 짧은 청크 허용
            10,     // minChunkLengthToEmbed: 더 낮은 임베딩 기준
            800,    // maxNumChunks: 더 많은 청크 생성
            true,    // keepSeparator: 문장 구조 유지
            DEFAULT_PUNCTUATION_MARKS
        );
    }
    
    /**
     * 속도 최적화용 설정으로 TokenTextSplitter 생성
     * @return 속도 최적화용 TokenTextSplitter
     */
    public static TokenTextSplitter createForSpeedOptimization() {
        return new TokenTextSplitter(
            400,    // chunkSize: 더 큰 청크
            100,    // minChunkSizeChars: 더 긴 최소 크기
            10,     // minChunkLengthToEmbed: 더 낮은 임베딩 기준
            1000,   // maxNumChunks: 더 많은 청크 허용
            true,    // keepSeparator: 문장 구조 유지
            DEFAULT_PUNCTUATION_MARKS
        );
    }
}
