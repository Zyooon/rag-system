package com.example.rag_project.storage;

import com.example.rag_project.constants.RagConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 문서 저장소 관리자
 * 
 * <p>이 클래스는 RAG 시스템의 문서 영속성을 담당합니다:</p>
 * <ul>
 *   <li>문서의 Redis 저장 및 조회</li>
 *   <li>문서 메타데이터 관리</li>
 *   <li>중복 문서 검사 및 방지</li>
 *   <li>문서 키 관리 및 검색</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>RedisTemplate을 통한 데이터 CRUD 작업</li>
 *   <li>문서 고유 키 생성 및 관리</li>
 *   <li>JSON 직렬화/역직렬화 처리</li>
 *   <li>중복 문서 감지 로직</li>
 * </ul>
 * 
 * <p><b>키 전략:</b></p>
 * <ul>
 *   <li>접두사: {@code rag:document:}</li>
 *   <li>고유 키: 파일명 + 내용 해시 기반</li>
 *   <li>메타데이터: JSON 형태로 저장</li>
 * </ul>
 * 
 * <p><b>의존성:</b> RedisTemplate&lt;String, Object&gt;</p>
 */

@Component
@RequiredArgsConstructor
public class RedisDocumentManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisDocumentManager.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /** Redis 문서 키 접두사 */
    private static final String DOCUMENT_KEY_PREFIX = RagConstants.REDIS_DOCUMENT_KEY_PREFIX;
    
    /** 메타데이터 필드명 상수들 */
    private static final String METADATA_SAVED_AT = RagConstants.METADATA_SAVED_AT;
    
    /**
     * Redis에 저장된 모든 문서 키 목록 조회
     * @return 문서 키 목록
     */
    public List<String> getAllDocumentKeys() {
        try {
            Set<String> keys = redisTemplate.keys(DOCUMENT_KEY_PREFIX + "*");
            if (keys != null) {
                return new ArrayList<>(keys);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Redis 키 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Redis에 저장된 모든 문서 조회
     * @return 문서 목록
     */
    public List<Map<String, Object>> getAllDocuments() {
        List<String> keys = getAllDocumentKeys();
        List<Map<String, Object>> documents = new ArrayList<>();
        
        for (String key : keys) {
            Map<String, Object> doc = getDocument(key);
            if (!doc.isEmpty()) {
                documents.add(doc);
            }
        }
        
        logger.info("Redis에서 {}개의 문서를 조회했습니다", documents.size());
        return documents;
    }
    
    /**
     * 특정 키로 문서 조회
     * @param key 문서 키
     * @return 문서 데이터
     */
    public Map<String, Object> getDocument(String key) {
        try {
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
            Map<String, Object> result = new HashMap<>();
            
            for (Map.Entry<Object, Object> entry : data.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            
            logger.debug("문서 조회 성공 ({}): {}개 필드", key, result.size());
            return result;
        } catch (Exception e) {
            logger.error("Redis 문서 조회 실패 ({}): {}", key, e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 문서 목록을 Redis에 저장
     * @param documents 저장할 문서 목록
     * @return 저장 결과 정보
     */
    public Map<String, Object> saveDocuments(List<Document> documents) {
        int savedCount = 0;
        int duplicateCount = 0;
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String key = DOCUMENT_KEY_PREFIX + i;
            
            // 중복 체크
            if (redisTemplate.hasKey(key)) {
                duplicateCount++;
                logger.info("중복 문서 건너뛰기: {}", key);
                continue;
            }
            
            Map<String, Object> documentData = new HashMap<>();
            documentData.put("content", doc.getText());
            documentData.put("metadata", doc.getMetadata());
            documentData.put("id", i);
            documentData.put(METADATA_SAVED_AT, LocalDateTime.now().toString());
            
            try {
                redisTemplate.opsForHash().putAll(key, documentData);
                savedCount++;
                logger.debug("문서 저장 성공: {}", key);
            } catch (Exception e) {
                logger.error("문서 저장 실패 ({}): {}", key, e.getMessage());
            }
        }
        
        String message = String.format("문서 저장 완료: %d개 저장, %d개 중복", 
                                       savedCount, duplicateCount);
        logger.info(message);
        
        return Map.of(
            "savedCount", savedCount,
            "duplicateCount", duplicateCount,
            "totalCount", documents.size(),
            "message", message
        );
    }
    
    /**
     * 특정 문서 삭제
     * @param key 삭제할 문서 키
     * @return 삭제 성공 여부
     */
    public boolean deleteDocument(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            if (result != null && result) {
                logger.info("문서 삭제 성공: {}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("문서 삭제 실패 ({}): {}", key, e.getMessage());
            return false;
        }
    }
    
    /**
     * 모든 문서 삭제
     * @return 삭제된 문서 수
     */
    public int deleteAllDocuments() {
        List<String> keys = getAllDocumentKeys();
        int deletedCount = 0;
        
        for (String key : keys) {
            if (deleteDocument(key)) {
                deletedCount++;
            }
        }
        
        logger.info("총 {}개의 문서를 삭제했습니다", deletedCount);
        return deletedCount;
    }
    
    /**
     * 문서 개수 조회
     * @return 저장된 문서 수
     */
    public int getDocumentCount() {
        return getAllDocumentKeys().size();
    }
    
    /**
     * Redis 연결 상태 확인
     * @return 연결 상태
     */
    public boolean isConnectionHealthy() {
        try {
            redisTemplate.opsForValue().get("health_check_" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            logger.warn("Redis 연결 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}
