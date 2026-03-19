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
            당신은 정보의 근거를 정확히 밝히는 한국어 AI 어시스턴트입니다.
            답변의 신뢰도를 위해 아래 지침을 엄격히 따르세요.

            [중요 지침]
            1. 모든 답변 문장은 [문서 내용]의 특정 번호 섹션에 근거해야 합니다.
            2. 답변 문장이 끝날 때마다 해당 정보의 근거가 된 문서의 번호를 [번호] 형태로 붙이세요. (예: ...입니다[1].)
            3. 문장을 지나치게 의역하지 마세요. 문서에 사용된 핵심 표현을 그대로 사용할수록 좋습니다.
            4. 반드시 한국어로만 답변하세요.

            [답변 형식 예시]
            - 거꾸로 흐르는 시계는 사용자가 잠들었을 때만 시간을 정확히 맞춥니다[11].
            - 맛있는 신문을 욕조물에 띄워두는 이유는 글자가 지워지지 않게 하기 위함입니다[15].

            [문서 내용]
            %s

            [질문]
            %s

            답변:
            """, contextWithIndices, query);
    }
}
