package com.example.rag_project.parser;

import com.example.rag_project.constants.ConfigConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 간단한 줄바꿈 기반 문서 파서
 * 
 * <p>이 파서는 다음과 같은 기준으로 문서를 분할합니다:</p>
 * <ul>
 *   <li>두 줄 바꿈(Double Newline)을 기준으로 문단 분할</li>
 *   <li>너무 긴 문단은 적절한 크기로 추가 분할</li>
 *   <li>최소한의 의미 단위 보장</li>
 * </ul>
 * 
 * <p><b>분할 기준:</b></p>
 * <ul>
 *   <li>\n\n (두 줄 바꿈) 기준 문단 분할</li>
 *   <li>500자 초과 시 추가 분할</li>
 *   <li>최소 50자 이상의 의미 있는 단위</li>
 * </ul>
 */
@Slf4j
@Component
public class SimpleLineParser {
    
    private static final int MAX_CHUNK_LENGTH = ConfigConstants.MAX_CHUNK_LENGTH;
    private static final int MIN_CHUNK_LENGTH = ConfigConstants.MIN_CHUNK_LENGTH;
    
    /**
     * 줄바꿈 기반으로 텍스트 파싱
     * @param content 파싱할 텍스트
     * @param baseMetadata 기본 메타데이터
     * @return 파싱된 문서 목록
     */
    public List<Document> parse(String content, Map<String, Object> baseMetadata) {
        List<Document> documents = new ArrayList<>();
        
        // 두 줄 바꿈으로 문단 분할
        String[] paragraphs = content.split(ConfigConstants.PARAGRAPH_SEPARATOR);
        
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            
            if (paragraph.length() < MIN_CHUNK_LENGTH) {
                // 너무 짧은 문단은 다음 문단과 합치기
                if (i + 1 < paragraphs.length) {
                    paragraph += ConfigConstants.PARAGRAPH_SEPARATOR + paragraphs[i + 1].trim();
                    i++; // 다음 문서는 건너뛰기
                }
            }
            
            if (paragraph.length() > MAX_CHUNK_LENGTH) {
                // 너무 긴 문단은 추가 분할
                List<String> chunks = splitLongParagraph(paragraph);
                for (String chunk : chunks) {
                    Document doc = createChunkDocument(chunk, i, baseMetadata);
                    if (doc != null) {
                        documents.add(doc);
                    }
                }
            } else if (paragraph.length() >= MIN_CHUNK_LENGTH) {
                // 적절한 크기의 문단
                Document doc = createChunkDocument(paragraph, i, baseMetadata);
                if (doc != null) {
                    documents.add(doc);
                }
            }
        }
        
        log.debug("SimpleLineParser: {}개 문단으로 분할됨", documents.size());
        return documents;
    }
    
    /**
     * 긴 문단 분할
     */
    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        
        // 문장 단위로 분할 시도
        String[] sentences = paragraph.split(ConfigConstants.SENTENCE_SPLIT_REGEX);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            
            if (currentChunk.length() + trimmedSentence.length() > MAX_CHUNK_LENGTH && currentChunk.length() >= MIN_CHUNK_LENGTH) {
                // 현재 청크 저장
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder(trimmedSentence);
            } else {
                // 현재 청크에 추가
                if (currentChunk.length() > 0) {
                    currentChunk.append(ConfigConstants.SPACE_SEPARATOR);
                }
                currentChunk.append(trimmedSentence);
            }
        }
        
        // 마지막 청크 추가
        if (currentChunk.length() >= MIN_CHUNK_LENGTH) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * 청크 문서 생성
     */
    private Document createChunkDocument(String content, int paragraphIndex, Map<String, Object> baseMetadata) {
        if (content.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> metadata = new java.util.HashMap<>(baseMetadata);
        
        // 청크 정보 추가
        metadata.put(ConfigConstants.METADATA_KEY_TITLE, extractTitle(content));
        metadata.put(ConfigConstants.METADATA_KEY_SECTION_TYPE, ConfigConstants.SECTION_TYPE_PARAGRAPH);
        metadata.put(ConfigConstants.METADATA_KEY_PARAGRAPH_INDEX, paragraphIndex);
        metadata.put(ConfigConstants.METADATA_KEY_CHUNK_LENGTH, content.length());
        
        return new Document(content.trim(), metadata);
    }
    
    /**
     * 내용에서 제목 추출
     */
    private String extractTitle(String content) {
        // 첫 문장 또는 첫 50자를 제목으로 사용
        String[] sentences = content.split(ConfigConstants.TITLE_SENTENCE_SPLIT_REGEX);
        
        if (sentences.length > 0 && sentences[0].trim().length() > ConfigConstants.MIN_TITLE_LENGTH) {
            String title = sentences[0].trim();
            if (title.length() > ConfigConstants.MAX_TITLE_LENGTH) {
                title = title.substring(0, ConfigConstants.TRUNCATED_TITLE_LENGTH) + ConfigConstants.ELLIPSIS;
            }
            return title;
        }
        
        // 첫 50자를 제목으로 사용
        String title = content.trim();
        if (title.length() > ConfigConstants.MAX_TITLE_LENGTH) {
            title = title.substring(0, ConfigConstants.TRUNCATED_TITLE_LENGTH) + ConfigConstants.ELLIPSIS;
        }
        return title;
    }
}
