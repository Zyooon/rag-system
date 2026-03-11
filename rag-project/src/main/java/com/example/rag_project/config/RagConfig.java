package com.example.rag_project.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        // 테스트를 위한 간단한 임베딩 모델 구현
        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                // 현재는 더미 임베딩 반환 - 나중에 실제 OpenAI 구현으로 교체
                float[] embedding = new float[1536]; // OpenAI 임베딩 차원
                // 간단한 해시 기반 임베딩 생성
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = (text.hashCode() % 1000) / 1000.0f;
                }
                return embedding;
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                // 테스트를 위한 간단한 구현
                List<org.springframework.ai.embedding.Embedding> embeddings = request.getInstructions().stream()
                    .map(text -> new org.springframework.ai.embedding.Embedding(embed(text), null, null))
                    .toList();
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getText());
            }
        };
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // SimpleVectorStore 생성
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
