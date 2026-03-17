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

        // 디버깅용 로그
        log.debug("Document metadata keys: {}", metadata.keySet());
        log.debug("Document metadata: {}", metadata);

        // 메타데이터에서 파일명 추출
        String filename = metadata.getOrDefault(CommonConstants.METADATA_KEY_FILENAME, "").toString();
        log.debug("Extracted filename: {}", filename);
        
        // 메타데이터에 파일명이 없으면 기본값 설정
        if (filename.isEmpty() || filename.equals(ConfigConstants.UNKNOWN)) {
            filename = ConfigConstants.UNKNOWN_FILENAME;
            log.debug("Using default filename: {}", filename);
        }
        
        source.setFilename(filename);
        
        // 문서 내용에서 제목 추출
        String content = document.getText();
        String[] lines = content.split(CommonConstants.NEWLINE);
        String documentTitle = "";
        
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            
            // 대괄호 제목
            if (line.startsWith(ConfigConstants.TITLE_PREFIX_BRACKET_START) && line.endsWith(ConfigConstants.TITLE_PREFIX_BRACKET_END)) {
                documentTitle = line.substring(1, line.length() - 1).trim();
                break;
            }
            // 마크다운 제목
            else if (line.startsWith(ConfigConstants.TITLE_PREFIX_MARKDOWN)) {
                documentTitle = line.substring(2).trim();
                break;
            }
            // 제목: 형식
            else if (line.startsWith(ConfigConstants.TITLE_PREFIX_KOREAN)) {
                documentTitle = line.substring(3).trim();
                break;
            }
        }
        
        // 제목을 찾지 못했으면 파일명 기반으로 생성
        if (documentTitle.isEmpty()) {
            documentTitle = filename.replace(ConfigConstants.TXT_EXTENSION, CommonConstants.EMPTY_STRING).replace(ConfigConstants.MD_EXTENSION, CommonConstants.EMPTY_STRING);
        }
        
        source.setContent(documentTitle);
        
        // 점수 및 ID 설정
        Double score = document.getScore();
        source.setSimilarityScore(score != null ? Double.parseDouble(new DecimalFormat(ConfigConstants.SCORE_DECIMAL_FORMAT).format(score)) : 0.0);
        
        Object chunkId = metadata.get(ConfigConstants.METADATA_KEY_CHUNK_ID);
        source.setChunkId(chunkId != null ? String.valueOf(chunkId) : null);

        return source;
    }
}
