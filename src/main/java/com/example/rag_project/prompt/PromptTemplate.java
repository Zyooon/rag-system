package com.example.rag_project.prompt;

/**
 * RAG 시스템에서 사용하는 프롬프트 템플릿을 관리하는 클래스
 * 
 * <p>이 클래스는 다양한 RAG 시나리오에 필요한 프롬프트 템플릿을 제공합니다:</p>
 * <ul>
 *   <li>기본 검색 답변 생성 프롬프트</li>
 *   <li>출처 정보 포함 답변 생성 프롬프트</li>
 * </ul>
 * 
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>문서 내용 기반 답변 생성</li>
 *   <li>출처 정보 참조 표시</li>
 *   <li>자연스러운 한국어 답변 유도</li>
 * </ul>
 */
public class PromptTemplate {
    
    /**
     * 기본 검색 답변 생성을 위한 프롬프트 템플릿
     * 
     * <p>이 템플릿은 다음과 같은 특징을 가집니다:</p>
     * <ul>
     *   <li>문서 내용만 사용하여 답변 생성</li>
     *   <li>직접적인 질문 답변 유도</li>
     *   <li>자연스러운 한국어 답변</li>
     *   <li>관련 정보 없을 경우 명시적 처리</li>
     * </ul>
     * 
     * @param context 검색된 문서 내용
     * @param query 사용자 질문
     * @return 포맷팅된 프롬프트 문자열
     */
    public static String createBasicSearchPrompt(String context, String query) {
        return String.format("""
            당신은 주어진 문서 내용을 바탕으로 질문에 답변하는 AI 어시스턴트입니다.
            
            [문서 내용]
            %s
            
            [사용자 질문]
            %s
            
            답변 지침:
            1. 문서 내용만 사용하여 답변하세요.
            2. 질문에 직접적으로 답변하세요.
            3. 자연스러운 한국어로 답변하세요.
            4. 문서에 관련 정보가 없다면 "문서에서 관련 정보를 찾을 수 없습니다"라고 답변하세요.
            
            답변:
            """, context, query);
    }
    
    /**
     * 출처 정보 포함 답변 생성을 위한 프롬프트 템플릿
     * 
     * <p>이 템플릿은 다음과 같은 특징을 가집니다:</p>
     * <ul>
     *   <li>각 정보의 출처를 참조 번호로 표시</li>
     *   <li>한국어 전용 답변 강제</li>
     *   <li>참조 번호 기반 출처 추적 지원</li>
     *   <li>자연스러운 답변 형식 유도</li>
     * </ul>
     * 
     * @param contextWithIndices 참조 번호가 포함된 문서 내용
     * @param query 사용자 질문
     * @return 포맷팅된 프롬프트 문자열
     */
    public static String createSearchWithSourcesPrompt(String contextWithIndices, String query) {
        return String.format("""
            당신은 한국어 AI 어시스턴트입니다. 반드시 한국어로만 답변하세요.
            
            [중요] 각 정보의 출처를 문장 끝에 [번호]로 반드시 표시해야 합니다.
            
            [문서 내용]
            %s
            
            [질문]
            %s
            
            [답변 형식 예시]
            맛있는 신문은 기사를 다 읽고 나면 먹을 수 있어요. 경제면은 스테이크 맛이 납니다.
            
            답변:
            """, contextWithIndices, query);
    }
}
