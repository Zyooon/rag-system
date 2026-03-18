package com.example.rag_project.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.example.rag_project.constants.ConfigConstants;
import redis.clients.jedis.JedisPooled;

/**
 * RAG 시스템 설정 클래스
 * 
 * <p>이 클래스는 RAG 시스템의 모든 핵심 설정을 관리합니다:</p>
 * <ul>
 *   <li><b>Ollama 연결</b> - 로컬 LLM 서버 연결 설정</li>
 *   <li><b>임베딩 모델</b> - 벡터화를 위한 임베딩 모델 설정</li>
 *   <li><b>챗봇 모델</b> - 답변 생성을 위한 챗봇 모델 설정</li>
 *   <li><b>벡터 저장소</b> - Redis 기반 벡터 저장소 설정</li>
 *   <li><b>Redis 연결</b> - Redis 서버 연결 및 직렬화 설정</li>
 * </ul>
 * 
 * <p><b>주요 설정:</b></p>
 * <ul>
 *   <li><b>기본 URL</b>: http://localhost:11434 (Ollama)</li>
 *   <li><b>임베딩 모델</b>: bge-m3</li>
 *   <li><b>챗봇 모델</b>: llama3</li>
 *   <li><b>Redis 포트</b>: 6379</li>
 * </ul>
 * 
 * <p><b>Bean 설정:</b></p>
 * <ul>
 *   <li><b>OllamaApi</b> - Ollama API 연결 객체</li>
 *   <li><b>EmbeddingModel</b> - 텍스트 임베딩 모델</li>
 *   <li><b>ChatModel</b> - 챗봇 대화 모델</li>
 *   <li><b>VectorStore</b> - 벡터 저장소 객체</li>
 *   <li><b>RedisTemplate</b> - Redis 데이터 접근 템플릿</li>
 * </ul>
 */
@Configuration
public class RagConfig {

    // Ollama API 설정 (로컬 서버 주소)
    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl(ConfigConstants.DEFAULT_OLLAMA_BASE_URL)
                .build();
    }

    // Ollama 임베딩 모델 설정
    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaEmbeddingOptions.builder()
                                .model(ConfigConstants.DEFAULT_OLLAMA_EMBEDDING_MODEL)
                                .build()
                )
                .build();
    }

    // 벡터 저장소 설정 (RedisVectorStore 사용)
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
        JedisPooled jedisPooled = new JedisPooled(jedisConnectionFactory.getHostName(), 
                                                jedisConnectionFactory.getPort());
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .initializeSchema(true)
                .prefix(ConfigConstants.VECTORSTORE_PREFIX)
                .indexName(ConfigConstants.VECTORSTORE_INDEX_NAME)
                .build();
    }

    // JedisConnectionFactory 설정
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = 
            new RedisStandaloneConfiguration();
        config.setHostName(ConfigConstants.DEFAULT_REDIS_HOST);
        config.setPort(ConfigConstants.DEFAULT_REDIS_PORT);
        config.setDatabase(ConfigConstants.DEFAULT_REDIS_DATABASE);
        
        JedisConnectionFactory factory = 
            new JedisConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    // RedisTemplate 설정
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());
        
        template.afterPropertiesSet();
        return template;
    }

    // Ollama 채팅 모델 설정
    @Bean
    public ChatModel chatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model(ConfigConstants.DEFAULT_OLLAMA_CHAT_MODEL)
                                .build()
                )
                .build();
    }
}