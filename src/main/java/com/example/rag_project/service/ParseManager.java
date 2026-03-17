package com.example.rag_project.service;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.parser.BulletParser;
import com.example.rag_project.parser.HierarchicalParser;
import com.example.rag_project.parser.SimpleLineParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 문서 파싱 관리 전문 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 <b>문서 파싱 전략</b>을 담당합니다:</p>
 * <ul>
 *   <li>🔍 <b>파서 선택</b> - 문서 유형에 맞는 최적 파서 자동 선택</li>
 *   <li>🏗️ <b>구조 분석</b> - 다양한 파싱 전략을 통한 문서 구조 분석</li>
 *   <li>🎯 <b>최적화</b> - 파싱 결과 품질 평가 및 최적 파서 선택</li>
 *   <li>🔄 <b>전략 패턴</b> - 여러 파서를 시도하여 최상의 결과 도출</li>
 * </ul>
 * 
 * <p><b>핵심 책임:</b></p>
 * <ul>
 *   <li><b>파서 관리</b>: Hierarchical, Bullet, SimpleLine 파서 통합 관리</li>
 *   <li><b>자동 선택</b>: 문서 내용 기반 최적 파서 자동 선택</li>
 *   <li><b>품질 평가</b>: 파싱 결과의 품질을 평가하고 최적 결과 선택</li>
 *   <li><b>전략 실행</b>: 여러 파싱 전략을 순차적으로 시도</li>
 * </ul>
 * 
 * <p><b>파싱 전략:</b></p>
 * <ol>
 *   <li>📋 <b>HierarchicalParser</b>: 제목/소제목 구조가 명확한 문서</li>
 *   <li>🔢 <b>BulletParser</b>: 번호 목록 형식의 문서</li>
 *   <li>📝 <b>SimpleLineParser</b>: 일반 텍스트 문서</li>
 * </ol>
 * 
 * <p><b>의존성:</b> HierarchicalParser, BulletParser, SimpleLineParser</p>
 * <p><b>출력물:</b> 파싱된 Document 객체 리스트</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParseManager {

    private final HierarchicalParser hierarchicalParser;
    private final BulletParser bulletParser;
    private final SimpleLineParser simpleLineParser;

    /**
     * 문서 내용에 가장 적합한 파서를 선택하여 파싱 실행
     * 
     * @param content 원본 문서 내용
     * @param filename 파일명 (메타데이터용)
     * @return 파싱된 Document 리스트
     */
    public List<Document> parseDocument(String content, String filename) {
        log.debug("문서 파싱 시작: " + filename + " (길이: " + content.length() + ")");
        
        // 기본 메타데이터 생성
        Map<String, Object> baseMetadata = Map.of(CommonConstants.METADATA_KEY_FILENAME, filename);
        
        // 1. HierarchicalParser 시도 (가장 선호)
        List<Document> hierarchicalResult = tryParseWithStrategy(
            content, baseMetadata, hierarchicalParser, CommonConstants.PARSER_HIERARCHICAL
        );
        
        if (isGoodParsingResult(hierarchicalResult, content)) {
            log.info("HierarchicalParser 선택됨: {} -> {}개 청크", filename, hierarchicalResult.size());
            return hierarchicalResult;
        }
        
        // 2. BulletParser 시도
        List<Document> bulletResult = tryParseWithStrategy(
            content, baseMetadata, bulletParser, CommonConstants.PARSER_BULLET
        );
        
        if (isGoodParsingResult(bulletResult, content)) {
            log.info("BulletParser 선택됨: {} -> {}개 청크", filename, bulletResult.size());
            return bulletResult;
        }
        
        // 3. SimpleLineParser 시도 (최후의 수단)
        List<Document> simpleResult = tryParseWithStrategy(
            content, baseMetadata, simpleLineParser, CommonConstants.PARSER_SIMPLE_LINE
        );
        
        log.info("SimpleLineParser 선택됨: {} -> {}개 청크", filename, simpleResult.size());
        return simpleResult;
    }

    /**
     * 특정 파서로 파싱 시도
     */
    private List<Document> tryParseWithStrategy(String content, Map<String, Object> baseMetadata, 
                                              Object parser, String strategyName) {
        try {
            List<Document> result;
            
            if (parser instanceof HierarchicalParser) {
                result = ((HierarchicalParser) parser).parse(content, baseMetadata);
            } else if (parser instanceof BulletParser) {
                result = ((BulletParser) parser).parse(content, baseMetadata);
            } else if (parser instanceof SimpleLineParser) {
                result = ((SimpleLineParser) parser).parse(content, baseMetadata);
            } else {
                throw new IllegalArgumentException(CommonConstants.ERROR_UNKNOWN_PARSER + parser.getClass().getSimpleName());
            }
            
            log.debug("{} 파싱 결과: {}개 청크 생성", strategyName, result.size());
            return result;
            
        } catch (Exception e) {
            log.warn("{} 파싱 실패: {}", strategyName, e.getMessage());
            return List.of();
        }
    }

    /**
     * 파싱 결과의 품질 평가
     */
    private boolean isGoodParsingResult(List<Document> documents, String originalContent) {
        if (documents.isEmpty()) {
            return false;
        }
        
        // 1. 최소 청크 수 확인 (너무 적게 나뉘면 안 좋음)
        if (documents.size() < 2) {
            return false;
        }
        
        // 2. 파싱된 내용의 총 길이가 원본의 50% 이상인지 확인
        int totalParsedLength = documents.stream()
            .mapToInt(doc -> doc.getText().length())
            .sum();
        
        double coverageRatio = (double) totalParsedLength / originalContent.length();
        if (coverageRatio < 0.5) {
            log.debug("파싱 커버리지 낮음: {}%", (int)(coverageRatio * 100));
            return false;
        }
        
        // 3. 각 청크의 평균 길이가 적절한지 확인 (너무 짧으면 안 좋음)
        double avgChunkLength = (double) totalParsedLength / documents.size();
        if (avgChunkLength < 20) {
            log.debug("청크 평균 길이 너무 짧음: {}자", (int)avgChunkLength);
            return false;
        }
        
        return true;
    }

    /**
     * 파서별 특징 분석 (디버깅용)
     */
    public void analyzeDocumentFeatures(String content) {
        log.info("문서 특징 분석 시작");
        
        // Hierarchical 특징
        long hierarchicalMarkers = content.lines()
            .filter(line -> line.matches(CommonConstants.PATTERN_HIERARCHICAL))
            .count();
        
        // Bullet 특징
        long bulletMarkers = content.lines()
            .filter(line -> line.matches(CommonConstants.PATTERN_BULLET))
            .count();
        
        // 전체 라인 수
        long totalLines = content.lines().count();
        
        log.info("전체 라인: {}", totalLines);
        log.info("Hierarchical 패턴: {}개 ({}%)", hierarchicalMarkers, 
                totalLines > 0 ? (hierarchicalMarkers * 100 / totalLines) : 0);
        log.info("Bullet 패턴: {}개 ({}%)", bulletMarkers, 
                totalLines > 0 ? (bulletMarkers * 100 / totalLines) : 0);
    }
}
