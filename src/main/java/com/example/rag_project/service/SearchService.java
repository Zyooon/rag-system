package com.example.rag_project.service;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.constants.MessageConstants;
import com.example.rag_project.dto.SourceInfo;
import com.example.rag_project.prompt.PromptTemplate;
import com.example.rag_project.repository.RedisSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 검색 및 답변 생성 전담 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 검색과 답변 생성 관련 모든 작업을 담당합니다:</p>
 * <ul>
 *   <li>벡터 저장소에서 유사 문서 검색</li>
 *   <li>LLM을 통한 자연스러운 한국어 답변 생성</li>
 *   <li>Redis에서 출처 정보 찾기 및 관리</li>
 *   <li>유사도 임계값 기반 필터링</li>
 *   <li>참조 번호 기반 출처 매칭</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>VectorStore를 통한 의미론적 검색</li>
 *   <li>ChatModel을 통한 LLM 답변 생성</li>
 *   <li>RedisSearchRepository를 통한 출처 정보 찾기</li>
 *   <li>문서 유사도 계산 및 매칭</li>
 *   <li>SourceInfo를 통한 출처 정보 관리</li>
 * </ul>
 * 
 * <p><b>설정값:</b></p>
 * <ul>
 *   <li>{@code rag.search.threshold}: 유사도 임계값 (기본값: 0.7)</li>
 *   <li>{@code rag.search.max-results}: 최대 검색 결과 수 (기본값: 5)</li>
 * </ul>
 * 
 * <p><b>의존성:</b> VectorStore, ChatModel, RedisSearchRepository</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final RedisSearchRepository redisSearchRepository;

    @Value("${" + ConfigConstants.CONFIG_SEARCH_THRESHOLD + ":" + ConfigConstants.DEFAULT_SEARCH_THRESHOLD + "}")
    private double similarityThreshold;

    @Value("${" + ConfigConstants.CONFIG_SEARCH_MAX_RESULTS + ":" + ConfigConstants.DEFAULT_MAX_RESULTS + "}")
    private int maxSearchResults;

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하고 출처 정보도 함께 반환하는 메서드
     */
    public Map<String, Object> searchAndAnswerWithSources(String query) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);

        if (relevantDocuments == null || relevantDocuments.isEmpty()) {
            return Map.of(
                ConfigConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_KNOWLEDGE_BASE,
                ConfigConstants.MAP_KEY_SOURCES, new SourceInfo()
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
                ConfigConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_RELEVANT_INFO,
                ConfigConstants.MAP_KEY_SOURCES, new SourceInfo()
            );
        }

        Set<String> processedChunks = new HashSet<>();
        List<SourceInfo> sources = relevantDocuments.stream()
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault(CommonConstants.METADATA_KEY_FILENAME, ConfigConstants.UNKNOWN).toString();
                return !filename.equals(CommonConstants.README_FILENAME);
            })
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault(CommonConstants.METADATA_KEY_FILENAME, "").toString();
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
            .map(doc -> {
                // 벡터 저장소 문서의 메타데이터가 유실되었으므로, Redis에서 원본 문서를 찾아서 SourceInfo 생성
                SourceInfo sourceInfo;
                try {
                    // 문서 내용으로 출처 정보 찾기
                    sourceInfo = findSourceInfoFromRedis(doc.getText());
                } catch (Exception e) {
                    log.warn("Failed to find source info from Redis, using fallback: {}", e.getMessage());
                    sourceInfo = SourceInfo.fromDocument(doc);
                }
                
                // 디버깅용 로그
                log.debug("Document metadata: {}, filename: {}", doc.getMetadata(), sourceInfo.getFilename());
                return sourceInfo;
            })
            .collect(Collectors.toList());

        log.debug("Sources list size: {}, first filename: {}", sources.size(), 
            sources.isEmpty() ? "empty" : sources.get(0).getFilename());

        StringBuilder contextWithIndices = new StringBuilder();
        for (int i = 0; i < relevantDocuments.size(); i++) {
            org.springframework.ai.document.Document doc = relevantDocuments.get(i);
            String filename = doc.getMetadata().getOrDefault(CommonConstants.METADATA_KEY_FILENAME, ConfigConstants.UNKNOWN).toString();
            String content = doc.getText();
            
            contextWithIndices.append(String.format("[%d] 파일명: %s\n내용: %s\n\n", i + 1, filename, content));
        }
        
        String context = contextWithIndices.toString();
        
        if (context.trim().isEmpty()) {
            return Map.of(
                ConfigConstants.MAP_KEY_ANSWER, MessageConstants.MSG_NO_RELEVANT_INFO_FOUND,
                ConfigConstants.MAP_KEY_SOURCES, new SourceInfo()
            );
        }
        
        String prompt = PromptTemplate.createSearchWithSourcesPrompt(context, query);

        try {
            String answer = chatModel.call(prompt);
            SourceInfo bestSource = findBestMatchingSource(answer, relevantDocuments, sources);
            
            return Map.of(
                ConfigConstants.MAP_KEY_ANSWER, answer,
                ConfigConstants.MAP_KEY_SOURCES, bestSource
            );
        } catch (Exception e) {
            SourceInfo bestSource = sources.isEmpty() ? new SourceInfo() : sources.get(0);
            return Map.of(
                ConfigConstants.MAP_KEY_ANSWER, MessageConstants.MSG_AI_ANSWER_ERROR + e.getMessage(),
                ConfigConstants.MAP_KEY_SOURCES, bestSource
            );
        }
    }

    /**
     * 출처 재계산: 답변의 참조 번호를 기반으로 정확한 출처 찾기
     */
    private SourceInfo findBestMatchingSource(String answer, List<Document> documents, List<SourceInfo> sources) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(answer);
        
        Set<Integer> refNumbers = new HashSet<>();
        while (matcher.find()) {
            refNumbers.add(Integer.parseInt(matcher.group(1)));
        }
        
        SourceInfo bestSource = new SourceInfo();
        
        // sources 리스트가 비어있으면 첫 번째 documents에서 SourceInfo 생성
        if (sources.isEmpty()) {
            log.debug("Sources list is empty, creating from first document");
            if (!documents.isEmpty()) {
                bestSource = SourceInfo.fromDocument(documents.get(0));
                log.debug("Created SourceInfo from first document: {}", bestSource.getFilename());
            }
            return bestSource;
        }
        
        log.debug("Found reference numbers: {}", refNumbers);
        
        for (int refNum : refNumbers) {
            int docIndex = refNum - 1;
            if (docIndex >= 0 && docIndex < sources.size()) {
                SourceInfo sourceInfo = sources.get(docIndex);
                
                if (sourceInfo.getFilename() != null && !sourceInfo.getFilename().isEmpty() && 
                    !sourceInfo.getFilename().equals(ConfigConstants.UNKNOWN_FILENAME)) {
                    bestSource = sourceInfo;
                    break;
                }
            }
        }
        
        // 적절한 출처를 찾지 못했다면 sources의 첫 번째 항목 사용
        if (bestSource.getFilename() == null || bestSource.getFilename().isEmpty()) {
            bestSource = sources.get(0);
        }
        
        return bestSource;
    }

    /**
     * 문서 내용으로 Redis에서 출처 정보를 찾는 메서드
     * 
     * @param documentText 검색된 문서 내용
     * @return 출처 정보 (SourceInfo 객체)
     */
    private SourceInfo findSourceInfoFromRedis(String documentText) {
        try {
            // Redis에서 모든 문서 조회
            List<Map<String, Object>> redisDocs = redisSearchRepository.getAllDocuments();
            return findSourceInfoFromDocuments(documentText, redisDocs);
        } catch (Exception e) {
            log.warn("Failed to find source info from Redis, using fallback: {}", e.getMessage());
            return createFallbackSourceInfo(documentText);
        }
    }

    /**
     * Redis 문서들 중에서 일치하는 출처 정보를 찾는 내부 메서드
     * 
     * @param content 검색된 문서 내용
     * @param redisDocs Redis에 저장된 문서 목록
     * @return 출처 정보
     */
    private SourceInfo findSourceInfoFromDocuments(String content, List<Map<String, Object>> redisDocs) {
        String normalizedContent = content.trim().toLowerCase();
        double bestMatchScore = 0.0;
        SourceInfo bestMatch = null;
        
        for (Map<String, Object> redisDoc : redisDocs) {
            String redisContent = (String) redisDoc.get(CommonConstants.KEY_CONTENT);
            if (redisContent == null) continue;
            
            String normalizedRedisContent = redisContent.trim().toLowerCase();
            
            // 유사도 계산 (간단한 문자열 유사도)
            double similarity = calculateContentSimilarity(normalizedContent, normalizedRedisContent);
            
            if (similarity > bestMatchScore && similarity > 0.3) { // 30% 이상 유사해야 매칭
                bestMatchScore = similarity;
                bestMatch = extractSourceInfoFromDocument(redisDoc, content);
                log.debug("Better match found: similarity={}, filename={}", similarity, bestMatch.getFilename());
            }
        }
        
        if (bestMatch != null) {
            log.debug("Best match found: filename={}, similarity={}", bestMatch.getFilename(), bestMatchScore);
            return bestMatch;
        }
        
        // 찾지 못했으면 기본 SourceInfo 반환
        return createFallbackSourceInfo(content);
    }

    /**
     * 문서 맵에서 출처 정보를 추출하는 메서드
     * 
     * @param doc Redis 문서 맵
     * @param originalContent 원본 문서 내용
     * @return 출처 정보
     */
    private SourceInfo extractSourceInfoFromDocument(Map<String, Object> doc, String originalContent) {
        Object metadataObj = doc.get(CommonConstants.KEY_METADATA);
        
        SourceInfo sourceInfo = new SourceInfo();
        
        if (metadataObj instanceof Map) {
            Map<?, ?> metaMap = (Map<?, ?>) metadataObj;
            String filename = metaMap.get(CommonConstants.METADATA_KEY_FILENAME) != null ? metaMap.get(CommonConstants.METADATA_KEY_FILENAME).toString() : ConfigConstants.UNKNOWN_FILENAME;
            sourceInfo.setFilename(filename);
        } else {
            sourceInfo.setFilename(ConfigConstants.UNKNOWN_FILENAME);
        }
        
        sourceInfo.setContent(originalContent);
        
        // chunkId 설정
        if (metadataObj instanceof Map) {
            Map<?, ?> metaMap = (Map<?, ?>) metadataObj;
            String chunkId = metaMap.get("chunkId") != null ? metaMap.get("chunkId").toString() : "";
            sourceInfo.setChunkId(chunkId.isEmpty() ? null : chunkId);
        }
        
        return sourceInfo;
    }

    /**
     * Fallback 출처 정보를 생성하는 메서드
     * 
     * @param documentText 문서 내용
     * @return 기본 출처 정보
     */
    private SourceInfo createFallbackSourceInfo(String documentText) {
        // 내용의 일부를 파일명으로 사용 (최대 50자)
        String fallbackName = documentText.length() > 50 ? 
            documentText.substring(0, 50) + "..." : documentText;
        
        SourceInfo sourceInfo = new SourceInfo();
        sourceInfo.setFilename(fallbackName);
        sourceInfo.setContent(documentText);
        
        log.debug("No matching document found in Redis, using fallback: {}", fallbackName);
        return sourceInfo;
    }

    /**
     * 두 문자열 간의 유사도 계산 (간단한 Jaccard 유사도 기반)
     * 
     * @param content1 첫 번째 문자열
     * @param content2 두 번째 문자열
     * @return 유사도 (0.0 ~ 1.0)
     */
    private double calculateContentSimilarity(String content1, String content2) {
        // 짧은 내용에 대해서는 정확한 일치 확인
        if (content1.length() < 100 && content2.length() < 100) {
            return content1.equals(content2) ? 1.0 : 0.0;
        }
        
        // 더 긴 내용에 대해서는 부분 문자열 매칭
        String shorter = content1.length() < content2.length() ? content1 : content2;
        String longer = content1.length() < content2.length() ? content2 : content1;
        
        // 20자 이상의 공통 부분 문자열 찾기
        int maxCommonLength = 0;
        for (int i = 0; i <= shorter.length() - 20; i++) {
            for (int j = 20; j <= Math.min(50, shorter.length() - i); j++) {
                String substring = shorter.substring(i, i + j);
                if (longer.contains(substring)) {
                    maxCommonLength = Math.max(maxCommonLength, j);
                }
            }
        }
        
        // 유사도 계산 (공통 부분의 길이 / 전체 길이)
        return (double) maxCommonLength / Math.max(content1.length(), content2.length());
    }
}
