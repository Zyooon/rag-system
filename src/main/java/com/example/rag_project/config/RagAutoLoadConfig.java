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

    /**
     * 애플리케이션 시작 시 Redis 연결상태 확인
     */
    @Bean
    public ApplicationRunner ragAutoLoadRunner() {
        return args -> {
            log.info("=== RAG 시스템 시작: Redis 연결 상태 점검 ===");
            
            try {
                // Redis 연결 테스트 (RagService에 이미 구현된 메서드 활용)
                boolean isConnected = ragService.testRedisConnection();
                
                if (isConnected) {
                    log.info("Redis 서버에 성공적으로 연결되었습니다.");
                    log.info("지식 베이스 준비 완료. 서비스 사용이 가능합니다.");
                } else {
                    log.warn("Redis 연결에 실패했습니다. 저장소 기능을 사용할 수 없습니다.");
                }
            } catch (Exception e) {
                log.error("Redis 연결 체크 중 오류 발생: {}", e.getMessage());
            }
        };
    }
   
}
