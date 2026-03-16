package com.example.rag_project.constants;

/**
 * Redis 관련 상수 클래스
 * 
 * <p>이 클래스는 RAG 시스템에서 Redis 관련 모든 상수를 관리합니다:</p>
 * <ul>
 *   <li>Redis 키 접두사 및 패턴</li>
 *   <li>벡터 인덱스 설정</li>
 * </ul>
 */
public final class RedisConstants {
    
    /** Redis 키 접두사 */
    public static final String REDIS_KEY_PREFIX = "rag:";
    
    /** Redis 문서 키 접두사 */
    public static final String REDIS_DOCUMENT_KEY_PREFIX = "rag:document:";
    
    /** Redis 임베딩 키 접두사 */
    public static final String REDIS_EMBEDDING_KEY_PREFIX = "embedding:";
    
    /** 벡터 인덱스 이름 */
    public static final String VECTOR_INDEX_NAME = "vector_index";
    
    /**
     * private 생성자 - 인스턴스화 방지
     */
    private RedisConstants() {
        throw new AssertionError("RedisConstants 클래스는 인스턴스화할 수 없습니다.");
    }
}
