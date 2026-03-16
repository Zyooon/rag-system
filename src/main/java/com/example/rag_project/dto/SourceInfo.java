package com.example.rag_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.text.DecimalFormat;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfo {
    
    private String filename;
    private String chunkId;
    private Double similarityScore;
    private String content;
    
    public static SourceInfo fromDocument(org.springframework.ai.document.Document document) {
        SourceInfo source = new SourceInfo();
        Map<String, Object> metadata = document.getMetadata();

        // 메타데이터에서 파일명 추출
        String filename = metadata.getOrDefault("filename", "").toString();
        
        // 메타데이터에 파일명이 없으면 기본값 설정
        if (filename.isEmpty() || filename.equals("알 수 없음")) {
            filename = "unknown.txt";
        }
        
        source.setFilename(filename);
        
        // 문서 내용에서 제목 추출
        String content = document.getText();
        String[] lines = content.split("\n");
        String documentTitle = "";
        
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            
            // 대괄호 제목
            if (line.startsWith("[") && line.endsWith("]")) {
                documentTitle = line.substring(1, line.length() - 1).trim();
                break;
            }
            // 마크다운 제목
            else if (line.startsWith("# ")) {
                documentTitle = line.substring(2).trim();
                break;
            }
            // 제목: 형식
            else if (line.startsWith("제목:")) {
                documentTitle = line.substring(3).trim();
                break;
            }
        }
        
        // 제목을 찾지 못했으면 파일명 기반으로 생성
        if (documentTitle.isEmpty()) {
            documentTitle = filename.replace(".txt", "").replace(".md", "");
        }
        
        source.setContent(documentTitle);
        
        // 점수 및 ID 설정
        Double score = document.getScore();
        source.setSimilarityScore(score != null ? Double.parseDouble(new DecimalFormat("#.##").format(score)) : 0.0);
        
        Object chunkId = metadata.get("chunk_id");
        source.setChunkId(chunkId != null ? String.valueOf(chunkId) : null);

        return source;
    }
}
