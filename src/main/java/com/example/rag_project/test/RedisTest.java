package com.example.rag_project.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTest implements CommandLineRunner {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔴 Redis 연결 테스트를 시작합니다...");
        
        try {
            // Redis 연결 테스트
            redisTemplate.opsForValue().set("test:connection", "Redis 연결 테스트 성공!");
            String result = (String) redisTemplate.opsForValue().get("test:connection");
            
            System.out.println("✅ Redis 연결 테스트 성공: " + result);
            
            // 테스트 데이터 정리
            redisTemplate.delete("test:connection");
            System.out.println("🧹 테스트 데이터를 정리했습니다.");
            
            // Redis에 저장된 문서 키 확인
            java.util.Set<String> keys = redisTemplate.keys("rag:document:*");
            System.out.println("🔍 Redis에 저장된 문서 키: " + keys.size() + "개");
            
            for (String key : keys) {
                System.out.println("  - " + key);
                
                // 문서 내용 확인
                java.util.Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                System.out.println("    📄 내용 필드: " + data.keySet());
                
                if (data.containsKey("content")) {
                    String content = data.get("content").toString();
                    System.out.println("    📝 내용 길이: " + content.length() + "자");
                    System.out.println("    📖 내용 미리보기: " + content.substring(0, Math.min(50, content.length())) + "...");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 테스트 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("🔴 Redis 연결 테스트를 종료합니다.");
    }
}
