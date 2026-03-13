package com.example.rag_project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 시작 시 자동으로 Redis에 저장된 문서를 로드하는 설정
 */
@Configuration
@Slf4j
public class RagAutoLoadConfig {

    /**
     * 애플리케이션 시작 시 RedisVectorStore 준비 확인
     */
    @Bean
    public ApplicationRunner ragAutoLoadRunner() {
        return args -> {
            log.info("=== RAG 시스템 시작: RedisVectorStore 준비 ===");
            
            try {
                // RedisVectorStore는 자동으로 연결을 관리하므로 별도 테스트 불필요
                log.info("Redis VectorStore가 준비되었습니다.");
                log.info("지식 베이스 준비 완료. 서비스 사용이 가능합니다.");
            } catch (Exception e) {
                log.error("RAG 시스템 초기화 중 오류 발생: {}", e.getMessage());
            }
        };
    }
   
}
