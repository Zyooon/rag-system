package com.example.rag_project.repository;

import com.example.rag_project.constants.CommonConstants;
import com.example.rag_project.constants.ConfigConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Redis 문서 데이터 접근 레포지토리
 * 
 * <p>이 클래스는 Redis에 저장된 문서 데이터에 직접 접근하는 역할을 담당합니다:</p>
 * <ul>
 *   <li><b>문서 CRUD</b> - 문서 생성, 조회, 수정, 삭제</li>
 *   <li><b>키 관리</b> - 문서 키 생성 및 관리</li>
 *   <li><b>데이터 조회</b> - 다양한 조건의 문서 조회</li>
 *   <li><b>예외 처리</b> - Redis 연결 및 데이터 처리 예외</li>
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
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisDocumentRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 문서 키 접두사 */
    private static final String DOCUMENT_KEY_PREFIX = ConfigConstants.REDIS_DOCUMENT_KEY_PREFIX;

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
            log.error("Redis 키 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
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
            
            log.debug("문서 조회 성공 ({}): {}개 필드", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis 문서 조회 실패 ({}): {}", key, e.getMessage());
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
            
            // 문서 내용과 메타데이터를 기반으로 고유한 키 생성
            String contentHash = Integer.toHexString(doc.getText().hashCode());
            String filename = (String) doc.getMetadata().getOrDefault(CommonConstants.METADATA_KEY_FILENAME, "unknown");
            String key = DOCUMENT_KEY_PREFIX + filename + "_" + contentHash + "_" + i;
            
            // 중복 체크
            if (redisTemplate.hasKey(key)) {
                duplicateCount++;
                log.info("중복 문서 건너뛰기: {}", key);
                continue;
            }
            
            Map<String, Object> documentData = new HashMap<>();
            documentData.put(CommonConstants.KEY_CONTENT, doc.getText());
            documentData.put(CommonConstants.KEY_METADATA, doc.getMetadata());
            documentData.put("id", i);
            documentData.put("saved_at", LocalDateTime.now().toString());
            
            try {
                redisTemplate.opsForHash().putAll(key, documentData);
                savedCount++;
                log.debug("문서 저장 성공: {}", key);
            } catch (Exception e) {
                log.error("문서 저장 실패 ({}): {}", key, e.getMessage());
            }
        }
        
        String message = String.format("문서 저장 완료: %d개 저장, %d개 중복", 
                                       savedCount, duplicateCount);
        log.info(message);
        
        return Map.of(
            ConfigConstants.MAP_KEY_SAVED_COUNT, savedCount,
            ConfigConstants.MAP_KEY_DUPLICATE_COUNT, duplicateCount,
            ConfigConstants.MAP_KEY_TOTAL_COUNT, documents.size(),
            ConfigConstants.MAP_KEY_MESSAGE, message
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
                log.info("문서 삭제 성공: {}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("문서 삭제 실패 ({}): {}", key, e.getMessage());
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
        
        log.info("총 {}개의 문서를 삭제했습니다", deletedCount);
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
            log.warn("Redis 연결 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 키 패턴으로 Redis 키들 삭제
     * @param keyPattern 키 패턴 (예: "rag:document:*")
     * @return 삭제된 키 수
     */
    public int deleteKeysByPattern(String keyPattern) {
        try {
            Set<String> keys = redisTemplate.keys(keyPattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("키 패턴 '{}'으로 {}개의 키를 삭제했습니다", keyPattern, keys.size());
                return keys.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("키 패턴 삭제 실패 ({}): {}", keyPattern, e.getMessage());
            return 0;
        }
    }

    /**
     * 여러 키 패턴으로 Redis 키들 삭제
     * @param keyPatterns 키 패턴 목록
     * @return 각 패턴별 삭제된 키 수 맵
     */
    public Map<String, Integer> deleteKeysByPatterns(List<String> keyPatterns) {
        Map<String, Integer> results = new HashMap<>();
        
        for (String pattern : keyPatterns) {
            int deletedCount = deleteKeysByPattern(pattern);
            results.put(pattern, deletedCount);
        }
        
        return results;
    }
}
