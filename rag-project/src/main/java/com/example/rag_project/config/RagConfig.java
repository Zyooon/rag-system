package com.example.rag_project.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        // 테스트를 위한 간단한 임베딩 모델 구현
        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                // 현재는 더미 임베딩 반환 - 나중에 실제 OpenAI 구현으로 교체
                // 노이즈 추가하여 zero norm 방지
                float[] embedding = new float[1536]; // OpenAI 임베딩 차원
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = (float) (Math.random() * 0.01); // 작은 랜덤 값
                }
                return embedding;
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                // 테스트를 위한 간단한 구현
                List<Embedding> embeddings = request.getInstructions().stream()
                    .map(text -> {
                        float[] embedding = new float[1536];
                        for (int i = 0; i < embedding.length; i++) {
                            embedding[i] = (float) (Math.random() * 0.01);
                        }
                        return new Embedding(embedding, null);
                    })
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
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
