package com.example.rag_project.parser;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문서의 구조를 분석하고 계층적 정보를 추출하는 파서
 * 구조화된 텍스트, 번호 목록, Markdown 형식을 지원
 */
public class HierarchicalParser {
    
    private String currentH1 = "";  // 대제목 (Level 1)
    private String currentH2 = "";  // 중제목 (Level 2)
    private String currentH3 = "";  // 소제목 (Level 3)
    
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
        List<Document> documents = new ArrayList<>();
        String[] lines = content.split("\n");
        
        StringBuilder currentSection = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (isHeading(trimmedLine)) {
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
                
            } else {
                // 일반 내용 추가
                if (currentSection.length() > 0) {
                    currentSection.append("\n");
                }
                currentSection.append(line);
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
                        currentH3 = "";
                        break;
                    case 2: // # 제목
                        currentH1 = title;
                        currentH2 = "";
                        currentH3 = "";
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
        if (title == null) return "";
        
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
            metadata.put("title", h1);
            metadata.put("heading_level", "1");
        } else if (!h2.isEmpty()) {
            metadata.put("title", h2);
            metadata.put("heading_level", "2");
        } else if (!h3.isEmpty()) {
            metadata.put("title", h3);
            metadata.put("heading_level", "3");
        }
        
        // 계층 정보 추가
        metadata.put("h1", h1);
        metadata.put("h2", h2);
        metadata.put("h3", h3);
        
        return new Document(content.trim(), metadata);
    }
}
