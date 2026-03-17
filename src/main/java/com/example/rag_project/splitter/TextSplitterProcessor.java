package com.example.rag_project.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 텍스트 분할 처리 전문 클래스
 * 
 * <p>이 클래스는 텍스트 분할의 비즈니스 로직을 담당합니다:</p>
 * <ul>
 *   <li>✂️ <b>문서 분할</b> - 다양한 전략으로 문서 분할</li>
 *   <li>📏 <b>길이 기반 분할</b> - 문서 길이에 따른 자동 분할</li>
 *   <li>🎯 <b>전략 선택</b> - 용도에 맞는 분할 전략 선택</li>
 *   <li>📊 <b>메타데이터 관리</b> - 분할 후 메타데이터 처리</li>
 * </ul>
 * 
 * <p><b>분할 전략:</b></p>
 * <ul>
 *   <li><b>기본 분할</b>: 일반 문서 처리용 표준 분할</li>
 *   <li><b>정밀 분할</b>: 검색 정확도를 높인 세분화 분할</li>
 *   <li><b>속도 분할</b>: 처리 속도를 높인 대용량 분할</li>
 * </ul>
 * 
 * <p><b>의존성:</b> TextSplitterConfig (설정값)</p>
 * <p><b>출력물:</b> 분할된 Document 객체 리스트</p>
 */
@Service
@Slf4j
public class TextSplitterProcessor {
    
    /**
     * 기본 설정으로 문서 리스트 분할
     * 
     * @param documents 분할할 문서 리스트
     * @return 분할된 문서 리스트
     */
    public List<Document> splitDocuments(List<Document> documents) {
        return splitWithSettings(documents, TextSplitterConfig.getDefaultSettings());
    }
    
    /**
     * 정밀 검색용 설정으로 문서 리스트 분할
     * 
     * @param documents 분할할 문서 리스트
     * @return 분할된 문서 리스트
     */
    public List<Document> splitDocumentsForPreciseSearch(List<Document> documents) {
        return splitWithSettings(documents, TextSplitterConfig.getPreciseSearchSettings());
    }
    
    /**
     * 속도 최적화용 설정으로 문서 리스트 분할
     * 
     * @param documents 분할할 문서 리스트
     * @return 분할된 문서 리스트
     */
    public List<Document> splitDocumentsForSpeed(List<Document> documents) {
        return splitWithSettings(documents, TextSplitterConfig.getSpeedOptimizationSettings());
    }
    
    /**
     * 단일 문서를 기본 설정으로 분할
     * 
     * @param document 분할할 문서
     * @return 분할된 문서 리스트
     */
    public List<Document> splitDocument(Document document) {
        return splitDocuments(List.of(document));
    }
    
    /**
     * 긴 문서만 분할 (800자 이상인 문서만)
     * 
     * @param documents 문서 리스트
     * @return 분할된 문서 리스트
     */
    public List<Document> splitLongDocuments(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        
        for (Document doc : documents) {
            List<Document> splitDocs = splitLongDocument(doc);
            result.addAll(splitDocs);
        }
        
        log.debug("긴 문서 분할 완료: {}개 -> {}개 청크", documents.size(), result.size());
        return result;
    }
    
    /**
     * 단일 긴 문서 분할 (800자 이상인 경우만 분할)
     * 
     * @param document 분할할 문서
     * @return 분할된 문서 리스트 (길이가 800자 미만이면 원본 반환)
     */
    public List<Document> splitLongDocument(Document document) {
        if (document.getText().length() <= TextSplitterConfig.LONG_DOCUMENT_THRESHOLD) {
            log.debug("문서 길이가 기준 미달: {}자 (기준: {}자)", 
                    document.getText().length(), TextSplitterConfig.LONG_DOCUMENT_THRESHOLD);
            return List.of(document);
        }
        
        log.debug("긴 문서 분할 시작: {}자", document.getText().length());
        return splitDocument(document);
    }
    
    /**
     * 지정된 설정으로 문서 분할
     * 
     * @param documents 분할할 문서 리스트
     * @param settings 분할 설정
     * @return 분할된 문서 리스트
     */
    private List<Document> splitWithSettings(List<Document> documents, TextSplitterConfig.SplitterSettings settings) {
        try {
            TokenTextSplitter splitter = createSplitter(settings);
            List<Document> result = splitter.apply(documents);
            
            log.debug("문서 분할 완료: {}개 -> {}개 청크 (설정: {})", 
                    documents.size(), result.size(), settings);
            
            return result;
        } catch (Exception e) {
            log.error("문서 분할 실패: {} - {}", settings, e.getMessage());
            return documents; // 실패 시 원본 반환
        }
    }
    
    /**
     * 설정으로 TokenTextSplitter 생성
     * 
     * @param settings 분할 설정
     * @return TokenTextSplitter 인스턴스
     */
    private TokenTextSplitter createSplitter(TextSplitterConfig.SplitterSettings settings) {
        return new TokenTextSplitter(
            settings.getChunkSize(),
            settings.getMinChunkSizeChars(),
            settings.getMinChunkLengthToEmbed(),
            settings.getMaxNumChunks(),
            settings.isKeepSeparator(),
            settings.getPunctuationMarks()
        );
    }
    
    /**
     * 문서 분할 통계 정보 생성
     * 
     * @param originalDocuments 원본 문서 리스트
     * @param splitDocuments 분할된 문서 리스트
     * @return 통계 정보 맵
     */
    public static Map<String, Object> createSplitStatistics(List<Document> originalDocuments, List<Document> splitDocuments) {
        int originalCount = originalDocuments.size();
        int splitCount = splitDocuments.size();
        double avgOriginalLength = originalDocuments.stream()
                .mapToInt(doc -> doc.getText().length())
                .average()
                .orElse(0.0);
        double avgSplitLength = splitDocuments.stream()
                .mapToInt(doc -> doc.getText().length())
                .average()
                .orElse(0.0);
        
        return Map.of(
                "original_count", originalCount,
                "split_count", splitCount,
                "split_ratio", (double) splitCount / originalCount,
                "avg_original_length", (int) avgOriginalLength,
                "avg_split_length", (int) avgSplitLength
        );
    }
    
    /**
     * 분할 품질 평가
     * 
     * @param splitDocuments 분할된 문서 리스트
     * @return 품질 평가 결과
     */
    public SplitQuality evaluateSplitQuality(List<Document> splitDocuments) {
        if (splitDocuments.isEmpty()) {
            return new SplitQuality(0.0, "분할된 문서가 없습니다", false);
        }
        
        // 평가 기준
        double avgLength = splitDocuments.stream()
                .mapToInt(doc -> doc.getText().length())
                .average()
                .orElse(0.0);
        
        boolean isOptimal = avgLength >= 50 && avgLength <= 500; // 적정 길이 범위
        double score = calculateQualityScore(avgLength);
        String message = createQualityMessage(avgLength, isOptimal);
        
        return new SplitQuality(score, message, isOptimal);
    }
    
    private double calculateQualityScore(double avgLength) {
        // 100-300자가 가장 이상적 (100점)
        if (avgLength >= 100 && avgLength <= 300) {
            return 100.0;
        }
        // 50-100자 또는 300-500자 (80점)
        if ((avgLength >= 50 && avgLength < 100) || (avgLength > 300 && avgLength <= 500)) {
            return 80.0;
        }
        // 20-50자 또는 500-800자 (60점)
        if ((avgLength >= 20 && avgLength < 50) || (avgLength > 500 && avgLength <= 800)) {
            return 60.0;
        }
        // 그 외 (40점)
        return 40.0;
    }
    
    private String createQualityMessage(double avgLength, boolean isOptimal) {
        if (isOptimal) {
            return String.format("좋은 분할 결과입니다 (평균 %.0f자)", avgLength);
        } else if (avgLength < 50) {
            return String.format("과도하게 분할되었습니다 (평균 %.0f자)", avgLength);
        } else if (avgLength > 500) {
            return String.format("분할이 부족합니다 (평균 %.0f자)", avgLength);
        } else {
            return String.format("일반적인 분할 결과입니다 (평균 %.0f자)", avgLength);
        }
    }
    
    /**
     * 분할 품질 평가 결과
     */
    public static class SplitQuality {
        private final double score;
        private final String message;
        private final boolean isOptimal;
        
        public SplitQuality(double score, String message, boolean isOptimal) {
            this.score = score;
            this.message = message;
            this.isOptimal = isOptimal;
        }
        
        public double getScore() { return score; }
        public String getMessage() { return message; }
        public boolean isOptimal() { return isOptimal; }
        
        @Override
        public String toString() {
            return String.format("SplitQuality{score=%.1f, optimal=%s, message='%s'}", 
                    score, isOptimal, message);
        }
    }
}
