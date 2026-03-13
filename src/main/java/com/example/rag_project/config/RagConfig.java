package com.example.rag_project.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RagConfig {

    @Value("${rag.vectorstore.type:simple}")
    private String vectorStoreType;

    // Ollama API 설정 (로컬 서버 주소)
    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    // Ollama 임베딩 모델 설정
    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        // application.yml과 일치하는 임베딩 모델 사용
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaEmbeddingOptions.builder()
                                .model("bge-m3") // 한국어 검색 성능이 뛰어난 모델로 일치
                                .build()
                )
                .build();
    }

    // 벡터 저장소 설정 (임시: SimpleVectorStore 사용 + Redis 백업)
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        System.out.println("Simple 벡터 저장소를 사용합니다 (Redis 백업 포함).");
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // RedisTemplate 설정 (문서 저장용)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Spring Boot 4.0 권장 직렬화 방식
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());
        
        template.afterPropertiesSet();
        return template;
    }

    // Ollama 채팅 모델 (LLM) 설정 추가
    @Bean
    public ChatModel chatModel(OllamaApi ollamaApi) {
        // 채팅 모델은 OllamaEmbeddingOptions가 아닌 OllamaOptions를 사용합니다.
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("llama3") // 사용자 질문에 답변을 생성할 모델명
                                // .temperature(0.7) // 필요하다면 창의성 수치를 조절할 수 있습니다.
                                .build()
                )
                .build();
    }
}