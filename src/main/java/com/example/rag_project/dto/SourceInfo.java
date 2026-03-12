package com.example.rag_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.text.DecimalFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfo {
    private String filename;
    private Integer chunkId;
    private Double similarityScore;
    private String content;
    
    public static SourceInfo fromDocument(org.springframework.ai.document.Document document) {
        SourceInfo source = new SourceInfo();
        source.setFilename(document.getMetadata().getOrDefault("filename", "알 수 없음").toString());
        
        // chunk_id 설정
        Object chunkId = document.getMetadata().get("chunk_id");
        if (chunkId != null) {
            source.setChunkId(((Number) chunkId).intValue());
        }
        
        // 소수점 둘째 자리까지 포맷팅
        Double score = document.getScore();
        if (score != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            source.setSimilarityScore(Double.parseDouble(df.format(score)));
        } else {
            source.setSimilarityScore(0.0);
        }
        
        source.setContent(document.getText());
        return source;
    }
}
