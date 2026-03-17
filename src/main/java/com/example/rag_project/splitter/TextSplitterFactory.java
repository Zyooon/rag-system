package com.example.rag_project.splitter;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * TextSplitter 생성을 위한 순수 팩토리 클래스
 * 
 * <p>이 클래스는 TextSplitter 인스턴스 생성만 담당합니다:</p>
 * <ul>
 *   <li>🏭 <b>인스턴스 생성</b> - 다양한 설정으로 TokenTextSplitter 생성</li>
 *   <li>⚙️ <b>설정 적용</b> - TextSplitterConfig의 설정값 적용</li>
 *   <li>🔧 <b>전략 제공</b> - 다양한 분할 전략용 Splitter 제공</li>
 * </ul>
 * 
 * <p><b>제공되는 Splitter:</b></p>
 * <ul>
 *   <li><b>기본 Splitter</b>: 일반 문서 처리용</li>
 *   <li><b>정밀 Splitter</b>: 정밀 검색용</li>
 *   <li><b>속도 Splitter</b>: 속도 최적화용</li>
 *   <li><b>커스텀 Splitter</b>: 사용자 정의 설정용</li>
 * </ul>
 * 
 * <p><b>의존성:</b> TextSplitterConfig (설정값)</p>
 * <p><b>출력물:</b> TokenTextSplitter 인스턴스</p>
 */
@Component
public class TextSplitterFactory {
    
    /**
     * 기본 설정으로 TokenTextSplitter 생성
     * @return 기본 설정이 적용된 TokenTextSplitter
     */
    public TokenTextSplitter createDefault() {
        TextSplitterConfig.SplitterSettings settings = TextSplitterConfig.getDefaultSettings();
        return createFromSettings(settings);
    }
    
    /**
     * 정밀 검색용 설정으로 TokenTextSplitter 생성
     * @return 정밀 검색용 TokenTextSplitter
     */
    public TokenTextSplitter createForPreciseSearch() {
        TextSplitterConfig.SplitterSettings settings = TextSplitterConfig.getPreciseSearchSettings();
        return createFromSettings(settings);
    }
    
    /**
     * 속도 최적화용 설정으로 TokenTextSplitter 생성
     * @return 속도 최적화용 TokenTextSplitter
     */
    public TokenTextSplitter createForSpeedOptimization() {
        TextSplitterConfig.SplitterSettings settings = TextSplitterConfig.getSpeedOptimizationSettings();
        return createFromSettings(settings);
    }
    
    /**
     * 사용자 정의 설정으로 TokenTextSplitter 생성
     * 
     * @param chunkSize 청크 크기
     * @param minChunkSizeChars 최소 청크 문자 수
     * @param minChunkLengthToEmbed 임베딩 최소 길이
     * @param maxNumChunks 최대 청크 수
     * @param keepSeparator 구분자 유지 여부
     * @param punctuationMarks 구분자 목록
     * @return 사용자 정의 설정이 적용된 TokenTextSplitter
     */
    public TokenTextSplitter createCustom(
            int chunkSize, 
            int minChunkSizeChars, 
            int minChunkLengthToEmbed, 
            int maxNumChunks, 
            boolean keepSeparator, 
            java.util.List<Character> punctuationMarks) {
        
        TextSplitterConfig.SplitterSettings settings = new TextSplitterConfig.SplitterSettings(
            chunkSize, minChunkSizeChars, minChunkLengthToEmbed, 
            maxNumChunks, keepSeparator, punctuationMarks
        );
        
        return createFromSettings(settings);
    }
    
    /**
     * 설정 객체로부터 TokenTextSplitter 생성
     * 
     * @param settings 분할 설정
     * @return TokenTextSplitter 인스턴스
     */
    private TokenTextSplitter createFromSettings(TextSplitterConfig.SplitterSettings settings) {
        return new TokenTextSplitter(
            settings.getChunkSize(),
            settings.getMinChunkSizeChars(),
            settings.getMinChunkLengthToEmbed(),
            settings.getMaxNumChunks(),
            settings.isKeepSeparator(),
            settings.getPunctuationMarks()
        );
    }
}
