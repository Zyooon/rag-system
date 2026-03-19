package com.example.rag_project.dto;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.text.DecimalFormat;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SourceInfo {
    
    private String filename;
    private String chunkId;
    private Double similarityScore;
    private String content;
    
    public static SourceInfo fromDocument(org.springframework.ai.document.Document document) {
        SourceInfo source = new SourceInfo();
        Map<String, Object> metadata = document.getMetadata();

        // 문서 내용 먼저 가져오기
        String content = document.getText();
        
        // 디버깅용 로그
        log.info("Document metadata keys: {}", metadata.keySet());
        log.info("Document metadata: {}", metadata);
        log.info("Document content preview: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);

        // 메타데이터에서 파일명 추출
        String filename = metadata.getOrDefault(CommonConstants.METADATA_KEY_FILENAME, "").toString();
        log.info("Extracted filename: {}", filename);
        
        // 메타데이터에 파일명이 없으면 기본값 설정
        if (filename.isEmpty() || filename.equals(ConfigConstants.UNKNOWN)) {
            filename = ConfigConstants.UNKNOWN_FILENAME;
            log.debug("Using default filename: {}", filename);
        }
        
        source.setFilename(filename);
        
        // 문서 내용에서 제목 추출
        String[] lines = content.split(CommonConstants.NEWLINE);
        String documentTitle = "";
        
        log.info("First few lines for title extraction:");
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            log.info("Line {}: '{}'", i, lines[i]);
        }
        
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            
            // 테이블 헤더는 제목으로 처리하지 않음
            if (line.startsWith("|") && line.contains("|")) {
                continue;
            }
            
            // 대괄호 제목
            if (line.startsWith(ConfigConstants.TITLE_PREFIX_BRACKET_START) && line.endsWith(ConfigConstants.TITLE_PREFIX_BRACKET_END)) {
                documentTitle = line.substring(1, line.length() - 1).trim();
                log.info("Found bracket title: '{}'", documentTitle);
                break;
            }
            // 마크다운 제목
            else if (line.startsWith(ConfigConstants.TITLE_PREFIX_MARKDOWN)) {
                documentTitle = line.substring(2).trim();
                log.info("Found markdown title: '{}'", documentTitle);
                break;
            }
            // 제목: 형식
            else if (line.startsWith(ConfigConstants.TITLE_PREFIX_KOREAN)) {
                documentTitle = line.substring(3).trim();
                log.info("Found Korean title: '{}'", documentTitle);
                break;
            }
        }
        
        // 제목을 찾지 못했으면 파일명 기반으로 생성
        if (documentTitle.isEmpty()) {
            // 테이블 내용은 제외하고 파일명에서 제목 추출
            if (content.contains("|") && content.contains("ITEM_")) {
                // 테이블 내용에서 첫 번째 ITEM 내용 추출
                String[] tableLines = content.split("\n");
                for (String line : tableLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("|") && trimmedLine.contains("ITEM_")) {
                        // 테이블 행에서 ITEM 내용만 추출
                        String[] parts = trimmedLine.split("\\|");
                        if (parts.length >= 3) {
                            // ITEM_ID와 이름을 조합하여 content 생성
                            String itemId = parts[1].trim();
                            String itemName = parts[2].trim();
                            documentTitle = itemId + " " + itemName;
                            break;
                        }
                    }
                }
                
                // ITEM을 찾지 못한 경우 빈 문자열로 설정
                if (documentTitle.isEmpty()) {
                    documentTitle = "테이블 항목";
                }
            } else {
                documentTitle = filename.replace(ConfigConstants.TXT_EXTENSION, CommonConstants.EMPTY_STRING).replace(ConfigConstants.MD_EXTENSION, CommonConstants.EMPTY_STRING);
            }
            log.info("Using fallback title: '{}'", documentTitle);
        }
        
        source.setContent(documentTitle);
        log.info("Final SourceInfo - filename: '{}', content: '{}'", filename, documentTitle);
        
        // 점수 및 ID 설정
        Double score = document.getScore();
        source.setSimilarityScore(score != null ? Double.parseDouble(new DecimalFormat(ConfigConstants.SCORE_DECIMAL_FORMAT).format(score)) : 0.0);
        
        Object chunkId = metadata.get(ConfigConstants.METADATA_KEY_CHUNK_ID);
        source.setChunkId(chunkId != null ? String.valueOf(chunkId) : null);

        return source;
    }
}
