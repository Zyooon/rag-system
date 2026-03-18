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
 * 불릿(목록) 기반 문서 파서
 * 
 * <p>이 파서는 다음과 같은 불릿 형식을 인식하고 분할합니다:</p>
 * <ul>
 *   <li>- 항목</li>
 *   <li>* 항목</li>
 *   <li>• 항목</li>
 *   <li>1. 항목</li>
 *   <li>- **굵은글씨**: 내용</li>
 *   <li>기능: 내용</li>
 *   <li>특징: 내용</li>
 * </ul>
 * 
 * <p><b>분할 기준:</b></p>
 * <ul>
 *   <li>불릿 항목별로 분할</li>
 *   <li>연관된 내용은 그룹화</li>
 *   <li>표 형식도 지원</li>
 * </ul>
 */
@Slf4j
@Component
public class BulletParser implements DocumentParser {
    
    /**
     * 불릿 기반으로 텍스트 파싱
     * @param content 파싱할 텍스트
     * @param baseMetadata 기본 메타데이터
     * @return 파싱된 문서 목록
     */
    public List<Document> parse(String content, Map<String, Object> baseMetadata) {
        List<Document> documents = new ArrayList<>();
        String[] lines = content.split(CommonConstants.NEWLINE);

        StringBuilder currentSection = new StringBuilder();
        String currentParentTitle = ConfigConstants.DEFAULT_PARENT_TITLE;
        int sectionStartLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // [핵심] 번호로 시작하는 진짜 제목을 만났을 때만 섹션을 교체한다!
            if (isParentItem(trimmedLine)) {
                // 1. 이미 모인 내용이 있으면 저장 (강아지 정보가 다 모였을 때 저장됨)
                if (currentSection.length() > 0) {
                    savePreviousSection(documents, currentSection, currentParentTitle, sectionStartLine, baseMetadata);
                }
                
                // 2. 새 아이템 정보로 갱신
                currentParentTitle = extractTitle(trimmedLine);
                currentSection = new StringBuilder(line);
                sectionStartLine = i;
            } 
            else {
                // 번호가 없는 줄(불릿이 있든 없든)은 무조건 현재 섹션에 추가
                if (sectionStartLine == -1) sectionStartLine = i;
                if (currentSection.length() > 0) currentSection.append(CommonConstants.NEWLINE);
                currentSection.append(line);
            }
        }

        // 파일 마지막 조각 저장
        savePreviousSection(documents, currentSection, currentParentTitle, sectionStartLine, baseMetadata);
        
        log.debug("BulletParser: {}개 섹션으로 분할됨", documents.size());
        return documents;
    }
    
    /**
     * 이전 섹션 저장
     */
    private void savePreviousSection(List<Document> documents, StringBuilder section, String title, int startLine, Map<String, Object> baseMetadata) {
        if (section.length() > 0) {
            // 본문 맨 앞에 상위 맥락 주입!
            String finalContent = String.format(ConfigConstants.SECTION_FORMAT, title, section.toString());
            Document doc = createSectionDocument(finalContent, title, startLine, baseMetadata);
            if (doc != null) documents.add(doc);
            section.setLength(0);
        }
    }

    /**
     * 섹션 문서 생성
     */
    private Document createSectionDocument(String content, String title, int startLine, Map<String, Object> baseMetadata) {
        if (content.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> metadata = new java.util.HashMap<>(baseMetadata);
        metadata.put(ConfigConstants.METADATA_KEY_TITLE, title);
        metadata.put(ConfigConstants.METADATA_KEY_SECTION_TYPE, ConfigConstants.SECTION_TYPE_BULLET);
        metadata.put(ConfigConstants.METADATA_KEY_START_LINE, startLine);
        
        // filename 메타데이터가 없는 경우를 대비하여 명시적으로 설정
        if (!metadata.containsKey(CommonConstants.METADATA_KEY_FILENAME)) {
            Object filename = baseMetadata.get(CommonConstants.METADATA_KEY_FILENAME);
            if (filename != null) {
                metadata.put(CommonConstants.METADATA_KEY_FILENAME, filename);
            }
        }
        
        return new Document(content.trim(), metadata);
    }
    
    private boolean isParentItem(String line) {
        return line.matches("^\\d+[\\.\\)]\\s+.+");
    }

    /**
     * 라인에서 제목 추출
     */
    private String extractTitle(String line) {
        // 불릿 기호 제거
        String title = line.replaceAll("^[-*•]\\s+", "");
        title = title.replaceAll("^\\d+\\.\\s+", "");
        title = title.replaceAll("^\\d+\\)\\s+", "");
        
        // 굵은 글씨 처리
        if (title.contains("**")) {
            Matcher matcher = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(title);
            if (matcher.find()) {
                title = matcher.group(1);
            }
        }
        
        // 콜론 앞부분만 제목으로 사용
        int colonIndex = title.indexOf(':');
        if (colonIndex > 0 && colonIndex < ConfigConstants.COLON_LIMIT_LENGTH) {
            title = title.substring(0, colonIndex);
        }
        
        // 제목이 너무 길면 자르기
        if (title.length() > ConfigConstants.MAX_TITLE_LENGTH) {
            title = title.substring(0, ConfigConstants.TRUNCATED_TITLE_LENGTH) + ConfigConstants.ELLIPSIS;
        }
        
        return title.trim();
    }
    
    /**
     * 파서 이름 반환
     * @return "Bullet"
     */
    @Override
    public String getParserName() {
        return CommonConstants.PARSER_BULLET;
    }
}
