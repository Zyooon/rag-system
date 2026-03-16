package com.example.rag_project.service;

import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.constants.MetadataConstants;
import com.example.rag_project.constants.MessageConstants;
import com.example.rag_project.prompt.PromptTemplate;
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

    @Value("${" + ConfigConstants.CONFIG_SEARCH_THRESHOLD + ":" + ConfigConstants.DEFAULT_SEARCH_THRESHOLD + "}")
    private double similarityThreshold;

    @Value("${" + ConfigConstants.CONFIG_SEARCH_MAX_RESULTS + ":" + ConfigConstants.DEFAULT_MAX_RESULTS + "}")
    private int maxSearchResults;

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하는 메서드
     */
    public String searchAndAnswer(String query) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);
        
        if (relevantDocuments.isEmpty()) {
            return MessageConstants.MSG_NO_RELEVANT_INFO;
        }

        List<Document> filteredDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .collect(Collectors.toList());

        if (filteredDocuments.isEmpty()) {
            return MessageConstants.MSG_NO_RELIABLE_INFO;
        }
        
        String context = relevantDocuments.stream()
                .filter(doc -> {
                    String filename = doc.getMetadata().getOrDefault(MetadataConstants.METADATA_FILENAME, MetadataConstants.UNKNOWN).toString();
                    return !filename.equals(MessageConstants.README_FILENAME);
                })
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        
        if (context.trim().isEmpty()) {
            return MessageConstants.MSG_NO_RELEVANT_INFO;
        }
        
        String prompt = PromptTemplate.createBasicSearchPrompt(context, query);

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
                MetadataConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_KNOWLEDGE_BASE,
                MetadataConstants.MAP_KEY_SOURCES, new SourceInfo()
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
                MetadataConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_RELEVANT_INFO,
                MetadataConstants.MAP_KEY_SOURCES, new SourceInfo()
            );
        }

        Set<String> processedChunks = new HashSet<>();
        List<SourceInfo> sources = relevantDocuments.stream()
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault(MetadataConstants.METADATA_FILENAME, MetadataConstants.UNKNOWN).toString();
                return !filename.equals(MessageConstants.README_FILENAME);
            })
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault(MetadataConstants.METADATA_FILENAME, "").toString();
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
                MetadataConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_RELEVANT_INFO_FOUND,
                MetadataConstants.MAP_KEY_SOURCES, new SourceInfo()
            );
        }
        
        String prompt = PromptTemplate.createSearchWithSourcesPrompt(context, query);

        try {
            String answer = chatModel.call(prompt);
            SourceInfo bestSource = findBestMatchingSource(answer, relevantDocuments);
            
            return Map.of(
                MetadataConstants.MAP_KEY_ANSWER, answer,
                MetadataConstants.MAP_KEY_SOURCES, bestSource
            );
        } catch (Exception e) {
            SourceInfo bestSource = sources.isEmpty() ? new SourceInfo() : sources.get(0);
            return Map.of(
                MetadataConstants.MAP_KEY_ANSWER, MessageConstants.MSG_AI_ANSWER_ERROR + e.getMessage(),
                MetadataConstants.MAP_KEY_SOURCES, bestSource
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
        
        for (int refNum : refNumbers) {
            int docIndex = refNum - 1;
            if (docIndex >= 0 && docIndex < documents.size()) {
                Document candidateDoc = documents.get(docIndex);
                SourceInfo sourceInfo = SourceInfo.fromDocument(candidateDoc);
                
                if (sourceInfo.getFilename() != null && !sourceInfo.getFilename().isEmpty()) {
                    bestSource = sourceInfo;
                    break;
                }
            }
        }
        
        return bestSource;
    }
}
