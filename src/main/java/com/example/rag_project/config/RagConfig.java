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