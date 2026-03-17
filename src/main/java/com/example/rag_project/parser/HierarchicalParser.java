package com.example.rag_project.parser;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문서의 구조를 분석하고 계층적 정보를 추출하는 파서
 * 구조화된 텍스트, 번호 목록, Markdown 형식을 지원
 */
@Slf4j
@Component
public class HierarchicalParser implements DocumentParser {
    
    private String currentH1 = CommonConstants.EMPTY_STRING;  // 대제목 (Level 1)
    private String currentH2 = CommonConstants.EMPTY_STRING;  // 중제목 (Level 2)
    private String currentH3 = CommonConstants.EMPTY_STRING;  // 소제목 (Level 3)
    
    // 제목 패턴 정의 (마크다운 형식 우선)
    private static final Pattern[] HEADING_PATTERNS = {
        // 마크다운 제목 형식 (우선순위 높음)
        Pattern.compile("^###\\s+(.+)$"),         // ### 소소제목
        Pattern.compile("^##\\s+(.+)$"),          // ## 소제목  
        Pattern.compile("^#\\s+(.+)$"),           // # 제목
        
        // 마크다운 목록 형식
        Pattern.compile("^-\\s+\\*\\*(.+?)\\*\\*:\\s*(.+)$"), // Markdown 굵은 글씨 항목
        Pattern.compile("^-\\s+(.+)$"),            // 일반 목록 항목
        
        // 기타 구조화된 형식
        Pattern.compile("^\\d+\\.\\d+\\.\\s+(.+)$"), // 1.1. 소제목
        Pattern.compile("^\\d+\\.\\s+(.+)$"),     // 1. 제목
        Pattern.compile("^\\[(.+)\\]$"),            // [제목] - 대괄호 제목
        Pattern.compile("^제목:\\s*(.+)$"),       // 제목: 내용
        Pattern.compile("^\\|.+\\|$")            // 표 형식 (테이블)
    };
    
    /**
     * 텍스트를 구조화된 문서 목록으로 파싱
     * @param content 파싱할 텍스트 내용
     * @param baseMetadata 기본 메타데이터
     * @return 구조화된 문서 목록
     */
    public List<Document> parse(String content, Map<String, Object> baseMetadata) {
        // 불릿형 데이터를 위한 부모-자식 구조 파싱
        if (containsBulletsWithHeaders(content)) {
            return parseBulletWithHeader(content, baseMetadata);
        }
        
        // Markdown 형식 감지 및 처리
        MarkdownParser markdownParser = new MarkdownParser();
        if (markdownParser.isMarkdownDocument(content)) {
            return markdownParser.parse(content, baseMetadata);
        }
        
        // 기존的一般 파싱 로직
        return parseGeneralDocument(content, baseMetadata);
    }
    
    /**
     * 라인이 제목인지 확인
     */
    private boolean isHeading(String line) {
        if (line.isEmpty()) return false;
        
        for (Pattern pattern : HEADING_PATTERNS) {
            if (pattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 라인이 목록 항목인지 확인
     */
    private boolean isListItem(String line) {
        if (line.isEmpty()) return false;
        
        // 목록 항목 패턴 확인
        return line.matches("^-\\s+(.+)$") ||           // - 항목
               line.matches("^\\d+\\.\\s+(.+)$") ||      // 1. 항목
               line.matches("^\\*\\s+(.+)$") ||          // * 항목
               line.matches("^•\\s+(.+)$") ||            // • 항목
               line.startsWith("- **") ||                // - **굵은글씨**
               line.startsWith("기능:") ||               // 기능: 내용
               line.startsWith("특징:") ||               // 특징: 내용
               line.startsWith("효능:") ||               // 효능: 내용
               line.startsWith("배터리:") ||             // 배터리: 내용
               line.matches("^[가-힣]+:.*$");           // 한글 단어: 내용
    }
    
    /**
     * 제목 정보 업데이트
     */
    private void updateHeadings(String line) {
        for (int i = 0; i < HEADING_PATTERNS.length; i++) {
            Pattern pattern = HEADING_PATTERNS[i];
            Matcher matcher = pattern.matcher(line);
            
            if (matcher.matches()) {
                String title = matcher.groupCount() > 0 ? matcher.group(1) : line;
                title = cleanTitle(title);
                
                switch (i) {
                    case 0: // ### 소소제목
                        currentH3 = title;
                        break;
                    case 1: // ## 소제목
                        currentH2 = title;
                        currentH3 = CommonConstants.EMPTY_STRING;
                        break;
                    case 2: // # 제목
                        currentH1 = title;
                        currentH2 = CommonConstants.EMPTY_STRING;
                        currentH3 = CommonConstants.EMPTY_STRING;
                        break;
                    case 3: // 굵은 글씨 항목
                    case 4: // 일반 목록 항목
                    case 5: // 1.1. 소제목
                    case 6: // 1. 제목
                    case 7: // [제목]
                    case 8: // 제목:
                    case 9: // 표
                        // 목록이나 기타 항목은 현재 레벨 유지
                        break;
                }
                break;
            }
        }
    }
    
    /**
     * 제목에서 불필요한 기호 제거
     */
    private String cleanTitle(String title) {
        if (title == null) return CommonConstants.EMPTY_STRING;
        
        return title
            .replaceAll("^#+\\s*", "")      // 마크다운 # 제거
            .replaceAll("^\\*+\\s*", "")     // 굵은 글씨 * 제거
            .replaceAll("^\\[|\\]$", "")      // 대괄호 제거
            .replaceAll("^제목:\\s*", "")     // "제목:" 접두사 제거
            .replaceAll("^\\d+\\.?\\d*\\.?\\s*", "") // 번호 접두사 제거
            .trim();
    }
    
    /**
     * 구조화된 문서 생성
     */
    private Document createDocument(String content, String h1, String h2, String h3, Map<String, Object> baseMetadata) {
        if (content.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> metadata = new java.util.HashMap<>(baseMetadata);
        
        // 제목 정보 추가 (우선순위대로)
        if (!h1.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h1);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_1);
        } else if (!h2.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h2);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_2);
        } else if (!h3.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h3);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_3);
        }
        
        // 계층 정보 추가
        metadata.put(ConfigConstants.METADATA_KEY_H1, h1);
        metadata.put(ConfigConstants.METADATA_KEY_H2, h2);
        metadata.put(ConfigConstants.METADATA_KEY_H3, h3);
        
        return new Document(content.trim(), metadata);
    }
    
    /**
     * 불릿형 데이터가 있는지 확인 (부모-자식 구조)
     * sample-bullet.txt 같은 진짜 불릿 문서만 true 반환
     * sample-odd.txt는 무조건 일반 불릿으로 처리
     */
    private boolean containsBulletsWithHeaders(String content) {
        String[] lines = content.split(CommonConstants.NEWLINE);
        boolean hasNumberedHeader = false;
        boolean hasBullets = false;
        int numberedHeaderCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // "9. 제목" 형태의 번호 헤더 확인
            if (trimmedLine.matches("^\\d+\\.\\s+.*")) {
                hasNumberedHeader = true;
                numberedHeaderCount++;
                log.debug("번호 헤더 발견: {}", trimmedLine);
            }
            // 불릿 항목 확인 (번호 헤더 제외)
            else if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || 
                     trimmedLine.startsWith("• ") || trimmedLine.matches("^\\d+\\)\\s+.*")) {
                hasBullets = true;
                log.debug("불릿 항목 발견: {}", trimmedLine);
            }
        }
        
        log.debug("containsBulletsWithHeaders 결과: hasNumberedHeader={}, hasBullets={}, numberedHeaderCount={}", 
                 hasNumberedHeader, hasBullets, numberedHeaderCount);
        
        // sample-odd.txt는 무조건 일반 불릿으로 처리 (번호 헤더 개수 무시)
        // 이 로직은 DocumentProcessingService에서 파일명을 확인하여 적용해야 함
        // 여기서는 일단 번호 헤더 개수 기준으로 처리
        
        return hasNumberedHeader && hasBullets;
    }
    
    /**
     * 불릿과 헤더가 함께 있는 문서 파싱 (부모-자식 구조)
     */
    private List<Document> parseBulletWithHeader(String content, Map<String, Object> baseMetadata) {
        List<Document> result = new ArrayList<>();
        String[] lines = content.split(CommonConstants.NEWLINE);
        
        log.debug("parseBulletWithHeader 시작: 총 {} 라인", lines.length);
        
        String currentHeader = "";
        StringBuilder currentBody = new StringBuilder();
        int headerLineNum = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            
            log.debug("라인 {}: {}", i, trimmedLine);
            
            // 시작점 찾기: 엄격한 정규식으로 새로운 아이템 시작 확인
            if (isNewItemStart(trimmedLine)) {
                log.debug("새로운 아이템 시작: {}", trimmedLine);
                
                // 이전 아이템 저장
                saveCurrentChunk(result, currentHeader, currentBody, headerLineNum, baseMetadata);
                
                // 신규 아이템 시작
                currentHeader = trimmedLine;
                currentBody = new StringBuilder();
                headerLineNum = i;
            } 
            // 내용 누적: 다음 숫자가 나오기 전까지의 모든 불릿(-)들을 해당 아이템 조각에 다 집어넣어
            else if (!trimmedLine.isEmpty() && !currentHeader.isEmpty()) {
                log.debug("내용 추가: {}", trimmedLine);
                if (currentBody.length() > 0) {
                    currentBody.append(CommonConstants.NEWLINE);
                }
                currentBody.append(line);
            }
        }
        
        // 마지막 아이템 저장
        saveCurrentChunk(result, currentHeader, currentBody, headerLineNum, baseMetadata);
        
        log.debug("parseBulletWithHeader 완료: {}개 아이템으로 분할됨", result.size());
        return result;
    }
    
    /**
     * 새로운 아이템 시작인지 엄격하게 확인 (가짜 제목 방지)
     */
    private boolean isNewItemStart(String line) {
        // 1. "숫자. " 형식인지 확인 (줄의 시작점에서 숫자+점+공백+내용)
        if (!line.matches("^\\d+\\.\\s+.+")) {
            return false;
        }
        
        // 2. 추가 검증: 제목은 보통 한 줄 내외(50자 미만)이므로 길이 체크
        if (line.length() > ConfigConstants.MAX_HEADER_LENGTH) {
            return false;
        }
        
        // 3. 불릿으로 시작하는 줄은 무조건 내용으로 간주 (숫자 무시)
        if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 현재 아이템 조각 저장
     */
    private void saveCurrentChunk(List<Document> result, String header, StringBuilder body, 
                                 int headerLineNum, Map<String, Object> baseMetadata) {
        if (header.length() > 0 && body.length() > 0) {
            String combinedContent = header + CommonConstants.NEWLINE + body.toString().trim();
            log.debug("아이템 저장: 헤더={}, 바디 길이={}", header, body.length());
            Document doc = createBulletDocument(combinedContent, header, body.toString(), 
                                              headerLineNum, baseMetadata);
            if (doc != null) {
                result.add(doc);
            }
        }
    }
    
    /**
     * 불릿 구조 문서 생성 (헤더+바디 정보 포함)
     */
    private Document createBulletDocument(String fullContent, String header, String body, 
                                       int headerLineNum, Map<String, Object> baseMetadata) {
        if (fullContent.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> metadata = new java.util.HashMap<>(baseMetadata);
        
        // 헤더에서 제목 추출
        String headerTitle = extractHeaderTitle(header);
        metadata.put(ConfigConstants.METADATA_KEY_TITLE, headerTitle);
        metadata.put(ConfigConstants.METADATA_KEY_SECTION_TYPE, ConfigConstants.SECTION_TYPE_BULLET_WITH_HEADER);
        metadata.put(ConfigConstants.METADATA_KEY_HEADER, header);
        metadata.put(ConfigConstants.METADATA_KEY_HEADER_LINE, headerLineNum);
        
        // 계층 정보 추가
        metadata.put(ConfigConstants.METADATA_KEY_H1, headerTitle);
        metadata.put(ConfigConstants.METADATA_KEY_H2, CommonConstants.EMPTY_STRING);
        metadata.put(ConfigConstants.METADATA_KEY_H3, CommonConstants.EMPTY_STRING);
        metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_1);
        
        // 바디 내용 요약 (검색 키워드로 활용)
        String bodySummary = body.length() > ConfigConstants.MAX_BODY_SUMMARY_LENGTH ? 
            body.substring(0, ConfigConstants.TRUNCATED_BODY_SUMMARY_LENGTH) + ConfigConstants.ELLIPSIS : body;
        metadata.put(ConfigConstants.METADATA_KEY_BODY_SUMMARY, bodySummary);
        
        // 본문 맨 앞에 상위 맥락 주입!
        String finalContent = String.format(ConfigConstants.SECTION_FORMAT, headerTitle, fullContent.trim());
        
        return new Document(finalContent, metadata);
    }
    
    /**
     * 헤더에서 제목 추출 ("9. 생각하는 전구" -> "생각하는 전구")
     */
    private String extractHeaderTitle(String header) {
        if (header == null || header.isEmpty()) {
            return CommonConstants.EMPTY_STRING;
        }
        
        // 번호 접두사 제거
        String title = header.replaceAll("^\\d+\\.\\s*", "");
        
        // 불필요한 기호 제거
        title = title.replaceAll("^#+\\s*", "");      // 마크다운 # 제거
        title = title.replaceAll("^\\*+\\s*", "");     // 굵은 글씨 * 제거
        title = title.replaceAll("^\\[|\\]$", "");      // 대괄호 제거
        title = title.replaceAll("^제목:\\s*", "");     // "제목:" 접두사 제거
        
        return title.trim();
    }
    
        
    /**
     * 일반 문서 파싱 (기존 로직)
     */
    private List<Document> parseGeneralDocument(String content, Map<String, Object> baseMetadata) {
        List<Document> documents = new ArrayList<>();
        String[] lines = content.split(CommonConstants.NEWLINE);
        
        StringBuilder currentSection = new StringBuilder();
        StringBuilder currentSubsection = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (isHeading(trimmedLine)) {
                // 현재 하위 섹션 저장
                if (currentSubsection.length() > 0) {
                    Document doc = createDocument(currentSubsection.toString(), currentH1, currentH2, currentH3, baseMetadata);
                    if (doc != null) {
                        documents.add(doc);
                    }
                    currentSubsection.setLength(0);
                }
                
                // 현재 섹션 저장
                if (currentSection.length() > 0) {
                    Document doc = createDocument(currentSection.toString(), currentH1, currentH2, currentH3, baseMetadata);
                    if (doc != null) {
                        documents.add(doc);
                    }
                    currentSection.setLength(0);
                }
                
                // 새로운 제목 설정
                updateHeadings(trimmedLine);
                
            } else if (isListItem(trimmedLine)) {
                // 목록 항목이면 현재 섹션에 추가하고, 하위 섹션으로도 분리
                if (currentSubsection.length() > 0) {
                    currentSubsection.append(CommonConstants.NEWLINE);
                }
                currentSubsection.append(line);
                
                // 현재 섹션에도 추가
                if (currentSection.length() > 0) {
                    currentSection.append(CommonConstants.NEWLINE);
                }
                currentSection.append(line);
                
            } else {
                // 일반 내용 추가
                if (currentSubsection.length() > 0) {
                    currentSubsection.append(CommonConstants.NEWLINE);
                }
                currentSubsection.append(line);
                
                if (currentSection.length() > 0) {
                    currentSection.append(CommonConstants.NEWLINE);
                }
                currentSection.append(line);
            }
        }
        
        // 마지막 하위 섹션 저장
        if (currentSubsection.length() > 0) {
            Document doc = createDocument(currentSubsection.toString(), currentH1, currentH2, currentH3, baseMetadata);
            if (doc != null) {
                documents.add(doc);
            }
        }
        
        // 마지막 섹션 저장
        if (currentSection.length() > 0) {
            Document doc = createDocument(currentSection.toString(), currentH1, currentH2, currentH3, baseMetadata);
            if (doc != null) {
                documents.add(doc);
            }
        }
        
        return documents;
    }
    
    /**
     * 파서 이름 반환
     * @return "Hierarchical"
     */
    @Override
    public String getParserName() {
        return CommonConstants.PARSER_HIERARCHICAL;
    }
}
