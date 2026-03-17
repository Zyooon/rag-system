package com.example.rag_project.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Redis 검색용 문서 데이터 접근 레포지토리
 * 
 * <p>이 클래스는 SearchService를 위해 Redis에 저장된 문서 데이터를 조회하는 역할을 담당합니다:</p>
 * <ul>
 *   <li>📄 <b>문서 조회</b> - 모든 문서 조회</li>
 *   <li>🔍 <b>키 관리</b> - 문서 키 목록 조회</li>
 *   <li>📊 <b>데이터 접근</b> - 개별 문서 조회</li>
 *   <li>🛡️ <b>예외 처리</b> - Redis 연결 및 데이터 처리 예외</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>RedisTemplate을 통한 데이터 조회 작업</li>
 *   <li>SearchService를 위한 읽기 전용 접근</li>
 *   <li>문서 내용 기반 검색 지원</li>
 * </ul>
 * 
 * <p><b>키 전략:</b></p>
 * <ul>
 *   <li>접두사: {@code rag:document:}</li>
 *   <li>메타데이터: JSON 형태로 저장</li>
 * </ul>
 * 
 * <p><b>의존성:</b> RedisTemplate&lt;String, Object&gt;</p>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisSearchRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 문서 키 접두사 */
    private static final String DOCUMENT_KEY_PREFIX = "rag:document:";

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
        
        log.info("Redis에서 {}개의 문서를 조회했습니다", documents.size());
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
            
            log.debug("문서 조회 성공 ({}): {}개 필드", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis 문서 조회 실패 ({}): {}", key, e.getMessage());
            return new HashMap<>();
        }
    }
}
