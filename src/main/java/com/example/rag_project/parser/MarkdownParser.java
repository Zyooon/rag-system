package com.example.rag_project.parser;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Markdown 형식 문서 전용 파서
 * 
 * <p>이 파서는 다음과 같은 Markdown 형식을 인식하고 계층적으로 파싱합니다:</p>
 * <ul>
 *   <li># 레벨 1 제목 (h1)</li>
 *   <li>## 레벨 2 제목 (h2)</li>
 *   <li>### 레벨 3 제목 (h3)</li>
 *   <li>- 불릿 목록</li>
 *   <li>일본 텍스트 내용</li>
 * </ul>
 * 
 * <p><b>파싱 전략:</b></p>
 * <ul>
 *   <li>h1, h2 제목 기준으로 섹션 분할</li>
 *   <li>h3 제목은 동일 섹션 내에서 유지</li>
 *   <li>계층 구조 정보를 메타데이터에 보존</li>
 * </ul>
 */
@Slf4j
@Component
public class MarkdownParser {
    
    /**
     * Markdown 문서인지 확인
     * @param content 확인할 텍스트 내용
     * @return Markdown 문서이면 true
     */
    public boolean isMarkdownDocument(String content) {
        String[] lines = content.split(CommonConstants.NEWLINE);
        int markdownHeadingCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            // Markdown 제목 형식 확인 (# ## ###)
            if (trimmedLine.matches(ConfigConstants.MARKDOWN_HEADING_PATTERN)) {
                markdownHeadingCount++;
            }
        }
        
        // 2개 이상의 Markdown 제목이 있으면 Markdown 문서로 간주
        return markdownHeadingCount >= ConfigConstants.MIN_MARKDOWN_HEADINGS;
    }
    
    /**
     * Markdown 문서 파싱 (h1, h2, h3 계층 구조 인식)
     * @param content 파싱할 Markdown 내용
     * @param baseMetadata 기본 메타데이터
     * @return 파싱된 문서 목록
     */
    public List<Document> parse(String content, Map<String, Object> baseMetadata) {
        List<Document> documents = new ArrayList<>();
        String[] lines = content.split(CommonConstants.NEWLINE);
        
        StringBuilder currentSection = new StringBuilder();
        String currentH1 = CommonConstants.EMPTY_STRING;
        String currentH2 = CommonConstants.EMPTY_STRING;
        String currentH3 = CommonConstants.EMPTY_STRING;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // h1, h2, h3 중 어떤 것이라도 만나면 섹션 분리 시작
            if (trimmedLine.matches(ConfigConstants.MARKDOWN_HEADING_PATTERN_1_3)) {
                
                // 1. 기존에 쌓인 섹션 저장
                if (currentSection.length() > 0) {
                    Document doc = createMarkdownDocument(currentSection.toString(), currentH1, currentH2, currentH3, baseMetadata);
                    if (doc != null) documents.add(doc);
                    currentSection.setLength(0);
                }
                
                // 2. 제목 업데이트
                if (trimmedLine.startsWith(ConfigConstants.H1_PREFIX)) {
                    currentH1 = trimmedLine.substring(2);
                    currentH2 = CommonConstants.EMPTY_STRING; 
                    currentH3 = CommonConstants.EMPTY_STRING;
                } else if (trimmedLine.startsWith(ConfigConstants.H2_PREFIX)) {
                    currentH2 = trimmedLine.substring(3);
                    currentH3 = CommonConstants.EMPTY_STRING;
                } else if (trimmedLine.startsWith(ConfigConstants.H3_PREFIX)) {
                    currentH3 = trimmedLine.substring(4);
                }

                // 3. 본문의 시작 부분에 계층 정보를 명시적으로 삽입 (가장 중요!)
                currentSection.append(String.format(ConfigConstants.HIERARCHY_FORMAT, currentH1, currentH2, currentH3, line));
                
            } else {
                if (currentSection.length() > 0) currentSection.append(CommonConstants.NEWLINE);
                currentSection.append(line);
            }
        }
        
        // 마지막 남은 조각 처리
        if (currentSection.length() > 0) {
            Document doc = createMarkdownDocument(currentSection.toString(), currentH1, currentH2, currentH3, baseMetadata);
            if (doc != null) documents.add(doc);
        }
        
        log.debug("MarkdownParser: {}개 섹션으로 분할됨", documents.size());
        return documents;
    }
    
    /**
     * Markdown 문서 생성
     * @param content 문서 내용
     * @param h1 레벨 1 제목
     * @param h2 레벨 2 제목
     * @param h3 레벨 3 제목
     * @param baseMetadata 기본 메타데이터
     * @return 생성된 문서
     */
    private Document createMarkdownDocument(String content, String h1, String h2, String h3, Map<String, Object> baseMetadata) {
        if (content.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> metadata = new java.util.HashMap<>(baseMetadata);
        
        // 제목 정보 설정 (우선순위: h3 > h2 > h1)
        if (!h3.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h3);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_3);
        } else if (!h2.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h2);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_2);
        } else if (!h1.isEmpty()) {
            metadata.put(ConfigConstants.METADATA_KEY_TITLE, h1);
            metadata.put(ConfigConstants.METADATA_KEY_HEADING_LEVEL, ConfigConstants.HEADING_LEVEL_1);
        }
        
        metadata.put(ConfigConstants.METADATA_KEY_SECTION_TYPE, ConfigConstants.SECTION_TYPE_MARKDOWN);
        metadata.put(ConfigConstants.METADATA_KEY_H1, h1);
        metadata.put(ConfigConstants.METADATA_KEY_H2, h2);
        metadata.put(ConfigConstants.METADATA_KEY_H3, h3);
        
        return new Document(content.trim(), metadata);
    }
}
