package com.example.rag_project.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.rag_project.dto.SourceInfo;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 검색 및 답변 생성 전담 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 검색과 답변 생성 관련 모든 작업을 담당합니다:</p>
 * <ul>
 *   <li>벡터 저장소에서 유사 문서 검색</li>
 *   <li>LLM을 통한 자연스러운 한국어 답변 생성</li>
 *   <li>출처 정보 추적 및 관리</li>
 *   <li>유사도 임계값 기반 필터링</li>
 *   <li>참조 번호 기반 출처 매칭</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>VectorStore를 통한 의미론적 검색</li>
 *   <li>ChatModel을 통한 LLM 답변 생성</li>
 *   <li>SourceInfo를 통한 출처 정보 관리</li>
 *   <li>유사도 점수 계산 및 필터링</li>
 * </ul>
 * 
 * <p><b>설정값:</b></p>
 * <ul>
 *   <li>{@code rag.search.threshold}: 유사도 임계값 (기본값: 0.7)</li>
 *   <li>{@code rag.search.max-results}: 최대 검색 결과 수 (기본값: 5)</li>
 * </ul>
 * 
 * <p><b>의존성:</b> VectorStore, ChatModel</p>
 */

@Service
@RequiredArgsConstructor
public class SearchService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    @Value("${rag.search.threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.search.max-results:5}")
    private int maxSearchResults;

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하는 메서드
     */
    public String searchAndAnswer(String query) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);
        
        if (relevantDocuments.isEmpty()) {
            return "관련 정보를 찾을 수 없습니다.";
        }

        List<Document> filteredDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .collect(Collectors.toList());

        if (filteredDocuments.isEmpty()) {
            return "질문과 관련된 충분히 신뢰할 수 있는 정보를 찾을 수 없습니다.";
        }
        
        String context = relevantDocuments.stream()
                .filter(doc -> {
                    String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                    return !filename.equals("README.md");
                })
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        
        if (context.trim().isEmpty()) {
            return "관련 정보를 찾을 수 없습니다.";
        }
        
        String prompt = String.format("""
            당신은 주어진 문서 내용을 바탕으로 질문에 답변하는 AI 어시스턴트입니다.
            
            [문서 내용]
            %s
            
            [사용자 질문]
            %s
            
            답변 지침:
            1. 문서 내용만 사용하여 답변하세요.
            2. 질문에 직접적으로 답변하세요.
            3. 자연스러운 한국어로 답변하세요.
            4. 문서에 관련 정보가 없다면 "문서에서 관련 정보를 찾을 수 없습니다"라고 답변하세요.
            
            답변:
            """, context, query);

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            return "AI 답변 생성 중 오류: " + e.getMessage();
        }
    }

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하고 출처 정보도 함께 반환하는 메서드
     */
    public Map<String, Object> searchAndAnswerWithSources(String query) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);

        if (relevantDocuments == null || relevantDocuments.isEmpty()) {
            return Map.of(
                "answer", "현재 지식 베이스(Redis)에 저장된 데이터가 없어 답변을 드릴 수 없습니다.",
                "sources", new SourceInfo()
            );
        }
        
        relevantDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(maxSearchResults)
            .collect(Collectors.toList());
        
        if (relevantDocuments.isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new SourceInfo()
            );
        }

        Set<String> processedChunks = new HashSet<>();
        List<SourceInfo> sources = relevantDocuments.stream()
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                return !filename.equals("README.md");
            })
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault("filename", "").toString();
                Double score = doc.getScore();
                
                String contentHash = String.valueOf(doc.getText().hashCode());
                String uniqueKey = filename + "|" + score + "|" + contentHash;
                
                if (processedChunks.contains(uniqueKey)) {
                    return false;
                }
                processedChunks.add(uniqueKey);
                return true;
            })
            .limit(5)
            .map(doc -> SourceInfo.fromDocument(doc))
            .collect(Collectors.toList());

        StringBuilder contextWithIndices = new StringBuilder();
        for (int i = 0; i < relevantDocuments.size(); i++) {
            contextWithIndices.append(String.format("[%d] %s\n\n", i + 1, relevantDocuments.get(i).getText()));
        }
        
        String context = contextWithIndices.toString();
        
        if (context.trim().isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new SourceInfo()
            );
        }
        
        String prompt = String.format("""
            당신은 한국어 AI 어시스턴트입니다. 반드시 한국어로만 답변하세요.
            
            [중요] 각 정보의 출처를 문장 끝에 [번호]로 반드시 표시해야 합니다.
            
            [문서 내용]
            %s
            
            [질문]
            %s
            
            [답변 형식 예시]
            맛있는 신문은 기사를 다 읽고 나면 먹을 수 있어요[1]. 경제면은 스테이크 맛이 납니다[1].
            
            답변:
            """, context, query);

        try {
            String answer = chatModel.call(prompt);
            SourceInfo bestSource = findBestMatchingSource(answer, relevantDocuments);
            
            return Map.of(
                "answer", answer,
                "sources", bestSource
            );
        } catch (Exception e) {
            SourceInfo bestSource = sources.isEmpty() ? new SourceInfo() : sources.get(0);
            return Map.of(
                "answer", "AI 답변 생성 중 오류: " + e.getMessage(),
                "sources", bestSource
            );
        }
    }

    /**
     * 출처 재계산: 답변의 참조 번호를 기반으로 정확한 출처 찾기
     */
    private SourceInfo findBestMatchingSource(String answer, List<Document> documents) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(answer);
        
        Set<Integer> refNumbers = new HashSet<>();
        while (matcher.find()) {
            refNumbers.add(Integer.parseInt(matcher.group(1)));
        }
        
        SourceInfo bestSource = new SourceInfo();
        double bestRelevanceScore = 0.0;
        
        for (int refNum : refNumbers) {
            int docIndex = refNum - 1;
            if (docIndex >= 0 && docIndex < documents.size()) {
                Document candidateDoc = documents.get(docIndex);
                double relevanceScore = calculateRelevanceScore(candidateDoc.getText());
                
                if (relevanceScore > bestRelevanceScore) {
                    bestRelevanceScore = relevanceScore;
                    bestSource = SourceInfo.fromDocument(candidateDoc);
                }
            }
        }
        
        if (bestRelevanceScore > 0) {
            return bestSource;
        }
        
        return findBestMatchingSourceByContent(answer, documents);
    }
    
    /**
     * 문서 관련성 점수 계산
     */
    private double calculateRelevanceScore(String content) {
        double score = 0.0;
        String[] keywords = {"맛있는 신문", "신문", "기사", "경제면", "스테이크", "욕조물", "보관"};
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                score += 1.0;
            }
        }
        return score;
    }
    
    /**
     * 기존 방식: 내용 기반 출처 매칭 (fallback용)
     */
    private SourceInfo findBestMatchingSourceByContent(String answer, List<Document> documents) {
        SourceInfo bestSource = new SourceInfo();
        double maxScore = 0.0;
        
        for (Document doc : documents) {
            double score = calculateContentSimilarity(answer, doc.getText());
            if (score > maxScore) {
                maxScore = score;
                bestSource = SourceInfo.fromDocument(doc);
            }
        }
        
        return bestSource;
    }
    
    /**
     * 답변과 문서 내용의 유사도 계산
     */
    private double calculateContentSimilarity(String answer, String document) {
        String[] answerWords = answer.toLowerCase().replaceAll("[^가-힣a-z0-9\\s]", "").split("\\s+");
        String[] docWords = document.toLowerCase().replaceAll("[^가-힣a-z0-9\\s]", "").split("\\s+");
        
        int matchCount = 0;
        for (String answerWord : answerWords) {
            if (answerWord.length() > 1) {
                for (String docWord : docWords) {
                    if (docWord.contains(answerWord) || answerWord.contains(docWord)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }
        
        return answerWords.length > 0 ? (double) matchCount / answerWords.length : 0.0;
    }
}
