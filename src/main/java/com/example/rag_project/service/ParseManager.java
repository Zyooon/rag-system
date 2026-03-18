package com.example.rag_project.service;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * 문서 파싱 관리 전문 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 <b>문서 파싱 전략</b>을 담당합니다:</p>
 * <ul>
 *   <li><b>파서 선택</b> - 문서 유형에 맞는 최적 파서 자동 선택</li>
 *   <li><b>구조 분석</b> - 다양한 파싱 전략을 통한 문서 구조 분석</li>
 *   <li><b>최적화</b> - 파싱 결과 품질 평가 및 최적 파서 선택</li>
 *   <li><b>전략 패턴</b> - 여러 파서를 시도하여 최상의 결과 도출</li>
 * </ul>
 * 
 * <p><b>핵심 책임:</b></p>
 * <ul>
 *   <li><b>파서 관리</b>: DocumentParser 인터페이스를 구현한 모든 파서 자동 관리</li>
 *   <li><b>자동 선택</b>: 문서 내용 기반 최적 파서 자동 선택</li>
 *   <li><b>품질 평가</b>: 파싱 결과의 품질을 평가하고 최적 결과 선택</li>
 *   <li><b>전략 실행</b>: 여러 파싱 전략을 순차적으로 시도</li>
 * </ul>
 * 
 * <p><b>파싱 전략:</b></p>
 * <ol>
 *   <li><b>HierarchicalParser</b>: 제목/소제목 구조가 명확한 문서</li>
 *   <li><b>MarkdownParser</b>: Markdown 형식 문서</li>
 *   <li><b>BulletParser</b>: 번호 목록 형식의 문서</li>
 *   <li><b>SimpleLineParser</b>: 일반 텍스트 문서</li>
 * </ol>
 * 
 * <p><b>의존성:</b> DocumentParser 구현체들 (Hierarchical, Markdown, Bullet, SimpleLine)</p>
 * <p><b>출력물:</b> 파싱된 Document 객체 리스트</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParseManager {

    // Spring이 자동으로 모든 DocumentParser 구현체를 주입
    private final List<DocumentParser> documentParsers;
    
    // 기본 파서 우선순위 설정 (확장 가능)
    private static final Map<String, Integer> DEFAULT_PARSER_PRIORITIES = new LinkedHashMap<>();
    static {
        DEFAULT_PARSER_PRIORITIES.put("Hierarchical", 1);
        DEFAULT_PARSER_PRIORITIES.put("Markdown", 2);
        DEFAULT_PARSER_PRIORITIES.put("Bullet", 3);
        DEFAULT_PARSER_PRIORITIES.put("SimpleLine", 4);
    }

    /**
     * 문서 내용에 가장 적합한 파서를 선택하여 파싱 실행
     * 
     * @param content 원본 문서 내용
     * @param filename 파일명 (메타데이터용)
     * @return 파싱된 Document 리스트
     */
    public List<Document> parseDocument(String content, String filename) {
        log.debug("문서 파싱 시작: {} (길이: {})", filename, content.length());
        
        // 기본 메타데이터 생성
        Map<String, Object> baseMetadata = Map.of(CommonConstants.METADATA_KEY_FILENAME, filename);
        
        // 사용 가능한 파서들을 우선순위 순서로 정렬
        List<DocumentParser> sortedParsers = getSortedParsers();
        
        // 각 파서를 시도하여 최상의 결과 선택
        for (DocumentParser parser : sortedParsers) {
            try {
                List<Document> result = parser.parse(content, baseMetadata);
                
                if (isGoodParsingResult(result, content)) {
                    log.info("{} 선택됨: {} -> {}개 청크", parser.getParserName(), filename, result.size());
                    return result;
                }
                
                log.debug("{} 파싱 결과 부적합: {}개 청크", parser.getParserName(), result.size());
                
            } catch (Exception e) {
                log.warn("{} 파싱 실패: {}", parser.getParserName(), e.getMessage());
            }
        }
        
        // 모든 파서가 실패한 경우, 가장 낮은 우선순위의 파서로 기본 파싱
        DocumentParser fallbackParser = getSortedParsers().get(getSortedParsers().size() - 1);
        log.warn("모든 파서 실패, {}로 기본 파싱: {}", fallbackParser.getParserName(), filename);
        return fallbackParser.parse(content, baseMetadata);
    }

    /**
     * 파서 우선순위에 따라 정렬된 파서 리스트 반환
     * @return 우선순위 순서로 정렬된 DocumentParser 리스트
     */
    private List<DocumentParser> getSortedParsers() {
        return documentParsers.stream()
            .sorted(Comparator.comparing(this::getParserPriority))
            .toList();
    }
    
    /**
     * 파서별 우선순위 반환 (낮을수록 높은 우선순위)
     * @param parser 우선순위를 계산할 파서
     * @return 우선순위 값
     */
    private int getParserPriority(DocumentParser parser) {
        String parserName = parser.getParserName();
        
        // 기본 우선순위 맵에서 찾기
        return Optional.ofNullable(DEFAULT_PARSER_PRIORITIES.get(parserName))
                .orElse(999); // 알 수 없는 파서는 가장 낮은 우선순위
    }
    
    /**
     * 사용 가능한 모든 파서 정보 반환 (디버깅용)
     * @return 파서 이름과 우선순위 정보
     */
    public Map<String, Integer> getAvailableParsers() {
        Map<String, Integer> parserInfo = new LinkedHashMap<>();
        
        for (DocumentParser parser : getSortedParsers()) {
            parserInfo.put(parser.getParserName(), getParserPriority(parser));
        }
        
        return parserInfo;
    }
    
    /**
     * 특정 파서로 파싱 시도 (테스트용)
     * @param parserName 사용할 파서 이름
     * @param content 파싱할 내용
     * @param baseMetadata 기본 메타데이터
     * @return 파싱 결과
     */
    public List<Document> parseWithSpecificParser(String parserName, String content, Map<String, Object> baseMetadata) {
        Optional<DocumentParser> targetParser = documentParsers.stream()
                .filter(parser -> parser.getParserName().equals(parserName))
                .findFirst();
        
        if (targetParser.isPresent()) {
            try {
                return targetParser.get().parse(content, baseMetadata);
            } catch (Exception e) {
                log.warn("{} 파싱 실패: {}", parserName, e.getMessage());
                return List.of();
            }
        } else {
            log.warn("파서를 찾을 수 없음: {}", parserName);
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
        
        // 전체 라인 수
        long totalLines = content.lines().count();
        log.info("전체 라인: {}", totalLines);
        
        // 각 파서별 특징 분석
        for (DocumentParser parser : documentParsers) {
            analyzeParserFeatures(content, parser, totalLines);
        }
    }
    
    /**
     * 특정 파서의 특징 분석
     * @param content 분석할 내용
     * @param parser 분석할 파서
     * @param totalLines 전체 라인 수
     */
    private void analyzeParserFeatures(String content, DocumentParser parser, long totalLines) {
        String parserName = parser.getParserName();
        
        switch (parserName) {
            case "Hierarchical", "Markdown" -> {
                long hierarchicalMarkers = content.lines()
                    .filter(line -> line.matches(CommonConstants.PATTERN_HIERARCHICAL))
                    .count();
                log.info("{} 패턴: {}개 ({}%)", parserName, hierarchicalMarkers, 
                        totalLines > 0 ? (hierarchicalMarkers * 100 / totalLines) : 0);
            }
            case "Bullet" -> {
                long bulletMarkers = content.lines()
                    .filter(line -> line.matches(CommonConstants.PATTERN_BULLET))
                    .count();
                log.info("{} 패턴: {}개 ({}%)", parserName, bulletMarkers, 
                        totalLines > 0 ? (bulletMarkers * 100 / totalLines) : 0);
            }
            case "SimpleLine" -> {
                // SimpleLineParser는 특별한 패턴이 없으므로 기본 정보만 출력
                log.info("{}: 일반 텍스트 파서 (특별한 패턴 없음)", parserName);
            }
            default -> {
                log.warn("알 수 없는 파서: {}", parserName);
            }
        }
    }
}
