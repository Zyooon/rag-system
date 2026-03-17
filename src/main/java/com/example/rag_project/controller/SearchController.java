package com.example.rag_project.controller;

import com.example.rag_project.dto.RagRequest;
import com.example.rag_project.dto.RagResponse;
import com.example.rag_project.dto.SourceInfo;
import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 검색 전용 컨트롤러
 * 
 * <p>이 컨트롤러는 RAG 시스템의 검색 관련 API를 담당합니다:</p>
 * <ul>
 *   <li>질문/답변 엔드포인트</li>
 *   <li>SearchService 직접 사용</li>
 *   <li>참조 번호 기반 출처 추적</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하는 엔드포인트
     * 
     * @param request 질문 요청 객체
     * @return 답변과 출처 정보 포함한 응답
     */
    @PostMapping(value = "/ask", produces = "application/json; charset=UTF-8")
    public ResponseEntity<RagResponse> query(@RequestBody RagRequest request) {
        try {
            // SearchService를 직접 사용하여 답변 생성
            Map<String, Object> result = searchService.searchAndAnswerWithSources(request.getQuery());
            String answer = (String) result.get(ConfigConstants.MAP_KEY_ANSWER);
            SourceInfo sources = (SourceInfo) result.get(ConfigConstants.MAP_KEY_SOURCES);
            
            return ResponseEntity.ok(RagResponse.success(answer, null, sources));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RagResponse.error("검색 실패: " + e.getMessage()));
        }
    }
}
