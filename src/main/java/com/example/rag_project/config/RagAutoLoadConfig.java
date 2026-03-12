package com.example.rag_project.config;

import com.example.rag_project.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 애플리케이션 시작 시 자동으로 Redis에 저장된 문서를 로드하는 설정
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RagAutoLoadConfig {

    private final RagService ragService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 애플리케이션 시작 시 Redis에 저장된 문서가 있는지 확인하고 자동으로 로드
     */
    @Bean
    public ApplicationRunner ragAutoLoadRunner() {
        return args -> {
            log.info("RAG 자동 로드 시작...");
            
            try {
                // 항상 파일 시스템에서 문서를 먼저 로드
                log.info("파일 시스템에서 문서를 로드합니다...");
                ragService.initializeDocuments();
                
                // Redis에 저장된 문서 키 확인
                Set<String> redisKeys = redisTemplate.keys("rag:document:*");
                
                if (redisKeys != null && !redisKeys.isEmpty()) {
                    log.info("Redis에 {}개의 추가 문서가 발견되었습니다.", redisKeys.size());
                    
                    // Redis에서 문서 데이터 로드
                    List<Map<String, Object>> documents = ragService.getAllRedisDocuments();
                    
                    if (!documents.isEmpty()) {
                        // 문서 내용을 벡터 저장소에 로드
                        ragService.loadDocumentsFromRedis();
                        
                        log.info("Redis 문서 추가 로드 완료: {}개 문서", documents.size());
                    }
                } else {
                    log.info("Redis에 저장된 추가 문서가 없습니다.");
                }
                
                log.info("RAG 문서 자동 로드 완료");
                log.info("이제 질의응답을 바로 사용할 수 있습니다.");
                
            } catch (Exception e) {
                log.error("RAG 자동 로드 실패: {}", e.getMessage(), e);
                log.info("수동으로 /api/rag/reload를 호출하여 문서를 다시 로드할 수 있습니다.");
            }
            
            log.info("RAG 자동 로드 종료");
        };
    }
}
