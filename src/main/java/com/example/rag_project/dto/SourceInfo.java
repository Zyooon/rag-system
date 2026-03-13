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
    
    /**
     * 제목에서 불필요한 기호를 제거하는 메서드
     */
    private static String cleanTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }
        
        String cleaned = title.trim();
        
        // 대괄호 제거
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        
        // 마크다운 기호 제거
        if (cleaned.startsWith("# ")) {
            cleaned = cleaned.substring(2).trim();
        }
        
        // 목록 기호 제거
        if (cleaned.startsWith("- ")) {
            cleaned = cleaned.substring(2).trim();
        }
        
        // "제목:" 접두사 제거
        if (cleaned.startsWith("제목:")) {
            cleaned = cleaned.substring(3).trim();
        }
        
        return cleaned;
    }
    
    public static SourceInfo fromDocument(org.springframework.ai.document.Document document) {
        SourceInfo source = new SourceInfo();
        
        // 디버깅: 전체 메타데이터 출력
        System.out.println("=== SourceInfo.fromDocument 디버깅 ===");
        System.out.println("전체 메타데이터: " + document.getMetadata());
        System.out.println("문서 내용: " + (document.getText().length() > 50 ? document.getText().substring(0, 50) + "..." : document.getText()));
        System.out.println("유사도 점수: " + document.getScore());
        
        // filename 설정 - null 체크 및 기본값 처리
        Object filenameObj = document.getMetadata().get("filename");
        System.out.println("filename 객체: " + filenameObj);
        String documentTitle = "";
        
        if (filenameObj != null) {
            documentTitle = filenameObj.toString();
            // 불필요한 기호 제거
            documentTitle = cleanTitle(documentTitle);
            source.setFilename(documentTitle);
            System.out.println("설정된 파일명: " + documentTitle);
        } else {
            // 메타데이터가 null이면 문서 내용에서 제목 추출
            String content = document.getText();
            String[] lines = content.split("\n");
            
            if (lines.length > 0 && !lines[0].trim().isEmpty()) {
                // 첫 줄이 문서 제목인 경우
                String firstLine = lines[0].trim();
                documentTitle = cleanTitle(firstLine);
                source.setFilename(documentTitle);
                System.out.println("문서 내용에서 추출한 제목: " + documentTitle);
            } else {
                source.setFilename("알 수 없음");
                System.out.println("문서 내용에서도 제목을 찾지 못해 '알 수 없음'으로 설정");
            }
        }
        
        // h1 정보 추출 - 메타데이터나 문서 내용에서
        Object h1Obj = document.getMetadata().get("h1");
        String h1Title = "";
        
        if (h1Obj != null && !h1Obj.toString().isEmpty()) {
            h1Title = cleanTitle(h1Obj.toString());
            System.out.println("메타데이터에서 추출한 h1: " + h1Title);
        } else {
            // 문서 내용에서 h1 추출
            String content = document.getText();
            String[] lines = content.split("\n");
            
            System.out.println("=== 문서 내용 분석 ===");
            for (int i = 0; i < Math.min(lines.length, 10); i++) {
                System.out.println("줄 " + (i+1) + ": " + lines[i]);
            }
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 여러 제목 패턴 찾기
                if (trimmedLine.matches("^\\d+\\.\\s.+")) {
                    // 번호 있는 제목 (예: "7. 춤추는 선인장")
                    h1Title = cleanTitle(trimmedLine);
                    System.out.println("문서 내용에서 추출한 h1 (번호): " + h1Title);
                    break;
                } else if (trimmedLine.startsWith("# ")) {
                    // 마크다운 제목 (예: "# 이상한 생물 및 장소 도감")
                    h1Title = cleanTitle(trimmedLine);
                    System.out.println("문서 내용에서 추출한 h1 (마크다운): " + h1Title);
                    break;
                } else if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    // 대괄호 제목 (예: "[세상의 모든 이상한 것들 사전]")
                    String bracketTitle = cleanTitle(trimmedLine);
                    // 이것이 문서 제목이 아니라 h1일 수도 있음
                    if (documentTitle.isEmpty() || documentTitle.equals("알 수 없음")) {
                        documentTitle = bracketTitle;
                        source.setFilename(documentTitle);
                        System.out.println("대괄호 내용을 문서 제목으로 설정: " + documentTitle);
                    }
                    break;
                }
            }
            
            if (h1Title.isEmpty()) {
                System.out.println("h1을 찾지 못했습니다. 전체 내용에서 번호 패턴 검색...");
                // 전체 내용에서 번호 패턴으로 검색
                for (String line : lines) {
                    if (line.trim().matches("^\\d+\\.\\s.+")) {
                        h1Title = cleanTitle(line.trim());
                        System.out.println("전체 검색으로 찾은 h1: " + h1Title);
                        break;
                    }
                }
            }
        }
        
        // content를 "문서제목 - 소제목" 형식으로 설정
        if (!h1Title.isEmpty()) {
            source.setContent(documentTitle + " - " + h1Title);
        } else {
            source.setContent(documentTitle);
        }
        
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
        } else {
            // chunk_id가 null일 경우 기본값 설정
            source.setChunkId(null);
        }
        
        // 소수점 둘째 자리까지 포맷팅
        Double score = document.getScore();
        if (score != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            source.setSimilarityScore(Double.parseDouble(df.format(score)));
        } else {
            source.setSimilarityScore(0.0);
        }
        
        // content 길이를 100자로 제한하고 말줄임표 추가
        String fullContent = document.getText();
        if (fullContent.length() > 100) {
            source.setContent(fullContent.substring(0, 100) + "...");
        } else {
            source.setContent(fullContent);
        }
        return source;
    }
}
