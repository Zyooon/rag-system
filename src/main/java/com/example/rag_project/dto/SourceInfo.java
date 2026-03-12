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
    private String chunkId;  // Integer에서 String으로 변경
    private Double similarityScore;
    private String content;
    
    public static SourceInfo fromDocument(org.springframework.ai.document.Document document) {
        SourceInfo source = new SourceInfo();
        source.setFilename(document.getMetadata().getOrDefault("filename", "알 수 없음").toString());
        
        // chunk_id 설정 (문자열 또는 숫자 처리)
        Object chunkId = document.getMetadata().get("chunk_id");
        if (chunkId != null) {
            if (chunkId instanceof String) {
                // 문자열 형태의 chunk_id (예: "sample-doc.txt_0")
                source.setChunkId((String) chunkId);
            } else if (chunkId instanceof Number) {
                // 숫자 형태의 chunk_id - 문자열로 변환
                source.setChunkId(String.valueOf(((Number) chunkId).intValue()));
            } else {
                // 그 외의 경우 문자열로 처리
                source.setChunkId(chunkId.toString());
            }
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
