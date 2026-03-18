package com.example.rag_project.parser;

import org.springframework.ai.document.Document;
import java.util.List;
import java.util.Map;

/**
 * 문서 파서 공통 인터페이스
 * 
 * <p>이 인터페이스는 RAG 시스템의 모든 문서 파서가 구현해야 하는 공통 계약을 정의합니다:</p>
 * <ul>
 *   <li><b>파싱 책임</b> - 다양한 형식의 문서를 구조화된 청크로 분할</li>
 *   <li><b>표준화</b> - 모든 파서가 동일한 파싱 시그니처를 따르도록 보장</li>
 *   <li><b>전략 패턴</b> - 런타임에 파서를 동적으로 선택하고 교체 가능</li>
 *   <li><b>일관성</b> - 모든 파서가 동일한 형식의 결과를 반환</li>
 * </ul>
 * 
 * <p><b>구현 클래스:</b></p>
 * <ul>
 *   <li>{@link HierarchicalParser} - 계층 구조 문서 파싱</li>
 *   <li>{@link MarkdownParser} - Markdown 형식 문서 파싱</li>
 *   <li>{@link BulletParser} - 목록/불릿 형식 문서 파싱</li>
 *   <li>{@link SimpleLineParser} - 일반 텍스트 문서 파싱</li>
 * </ul>
 * 
 * <p><b>사용 예시:</b></p>
 * <pre>{@code
 * DocumentParser parser = new HierarchicalParser();
 * List<Document> documents = parser.parse(content, metadata);
 * }</pre>
 */
public interface DocumentParser {
    
    /**
     * 문서 내용을 파싱하여 구조화된 Document 리스트로 변환
     * 
     * @param content 파싱할 원본 문서 내용
     * @param baseMetadata 문서에 추가할 기본 메타데이터 (파일명 등)
     * @return 파싱된 Document 객체 리스트. 파싱 실패 시 빈 리스트 반환
     * 
     * @throws IllegalArgumentException content가 null이거나 비어있을 경우
     * 
     * @implSpec 각 구현체는 자신의 전문 분야에 맞는 파싱 전략을 적용해야 합니다.
     *            파싱 결과는 일관된 메타데이터 구조를 가져야 합니다.
     */
    List<Document> parse(String content, Map<String, Object> baseMetadata);
    
    /**
     * 이 파서가 지정된 문서 내용을 처리할 수 있는지 확인
     * 
     * @param content 확인할 문서 내용
     * @return 처리 가능하면 true, 아니면 false
     * 
     * @implSpec 기본 구현은 true를 반환합니다. 필요한 경우 재정의하여
     *            파서별 호환성 검사 로직을 구현할 수 있습니다.
     */
    default boolean canHandle(String content) {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * 파서의 이름/타입을 반환
     * 
     * @return 파서를 식별하는 고유한 이름
     * 
     * @implSpec 각 구현체는 자신을 식별할 수 있는 고유한 이름을 반환해야 합니다.
     *            이 이름은 로깅, 디버깅, 모니터링에 사용됩니다.
     */
    String getParserName();
}
