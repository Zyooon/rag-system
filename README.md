# RAG 시스템 프로젝트

Spring Boot와 Spring AI를 기반으로, 문서 파싱·청킹·벡터 검색을 통해 문서 기반 질의응답을 수행하는 RAG 시스템입니다.

## 🚀 주요 기능

- 문서 파싱 및 벡터화
- 의미 기반 유사도 검색
- 참조 번호 기반 출처 추적
- 계층적 문서 구조 파싱
- Redis 기반 벡터 + 메타데이터 저장

## 🛠️ 기술 스택

- **Java 17**
- **Spring Boot**
- **Spring AI**
- **Ollama** (llama3, bge-m3)
- **Redis Stack**
- **Gradle**

## 📁 프로젝트 구조

```
- controller: API 엔드포인트
- service: RAG 처리 로직 (검색 / 생성)
- parser: 문서 구조 분석
- splitter: 텍스트 분할
- repository: Redis 데이터 관리
- config: 설정 클래스
```

## 구현도
- 문서 파싱 및 분할
<img width="661" height="481" alt="Image" src="https://github.com/user-attachments/assets/1b24ea10-5a25-413b-9fca-657181a4f707" />

<br><br>

- 검색 및 답변 생성
<img width="661" height="511" alt="Image" src="https://github.com/user-attachments/assets/5b7fac2b-7db3-4d1c-8b5a-bd6710540ba6" />

## 🚀 시작하기

### 사전 요구사항

1. **Java 17** 이상 설치
2. **Redis Stack Server** 실행
   ```bash
   docker run -d -p 6379:6379 redis/redis-stack-server:latest
   ```
3. **Ollama** 설치 및 모델 다운로드
   ```bash
   # Ollama 설치 후
   ollama pull llama3
   ollama pull bge-m3
   ```

### 실행 방법

1. 프로젝트 클론
   ```bash
   git clone <repository-url>
   cd rag-project
   ```

2. 애플리케이션 실행
   ```bash
   ./gradlew bootRun
   ```

3. 서버 접속
   - 기본 포트: 8080
   - API 엔드포인트: `http://localhost:8080/api/rag`

## 📚 API 사용법

### 검색 기능
```bash
# 질문/답변 (POST 방식)
POST /api/search
Content-Type: application/json

{
  "query": "네모난 사과의 특징은 무엇인가요?"
}

### 문서 관리 API
```bash
# 문서 저장
POST /api/rag/documents

# 시스템 상태 확인
GET /api/rag/status

# 벡터 저장소 초기화
DELETE /api/rag/documents

# 문서 재로드
PUT /api/rag/documents/reload
```

### 응답 예시
```json
{
  "success": true,
  "answer": "네모난 사과는 껍질이 보라색이며 맛은 민트초코 맛이 납니다. [1]",
  "sources": {
    "filename": "sample-odd.txt",
    "chunkId": "1",
    "similarityScore": 0.95,
    "content": "네모난 사과 (Square Apple)"
  }
}
```

## 📄 문서 형식

`documents/` 폴더에 다양한 형식의 문서를 저장할 수 있습니다:

- **번호 목록**: `1. 항목`, `2. 항목`
- **마크다운 제목**: `# 제목`, `## 소제목`
- **대괄호 제목**: `[제목]`
- **목록 항목**: `- 항목`
- **표 형식**: 테이블 구조 지원
- **혼합 형식**: 여러 형식의 조합 지원

## 🔧 핵심 설계

### 1. 이중 저장 구조
- VectorStore: 벡터 임베딩 저장 (검색용)
- Redis: 원본 + 메타데이터 저장 (출처 추적)
- 두 저장소 간 데이터 동기화

### 2. 문서 파싱
- 문서 구조(제목, 목록 등)를 기반으로 파싱
- 문서 형식에 따라 파서 자동 선택

### 3. 텍스트 분할 (Chunking)
- 의미 단위로 텍스트 분할
- 문서 특성에 맞는 분할 전략 적용

### 4. 출처 추적
- 참조 번호 기반으로 답변과 출처 매핑
- Redis 원본 데이터 기반 검증

### 5. 벡터 검색
- 임베딩 기반 유사도 검색
- 임계값 필터링 및 결과 정렬

## ⚙️ 설정

### application.properties
```properties
# Ollama 설정
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3
spring.ai.ollama.embedding.model=bge-m3

# Redis 설정
spring.data.redis.host=localhost
spring.data.redis.port=6379

# RAG 설정
rag.search.threshold=0.7
rag.search.max-results=5
```

## 🧪 테스트

샘플 문서가 `documents/` 폴더에 포함되어 있습니다:

- `sample-doc.txt`: 번호 목록 형식의 이상한 것들 사전
- `sample-markdown.txt`: 마크다운 형식 문서
- `sample-table.txt`: 표 형식 문서
- 기타 다양한 형식의 테스트 문서들

## 🔄 작동 원리

1. **문서 로딩**: FileManager가 파일 시스템에서 문서 읽기
2. **파서 선택**: ParseManager가 문서 특성에 맞는 최적 파서 자동 선택
3. **구조 분석**: 파서가 문서 구조를 분석하여 의미 단위로 분할
4. **텍스트 분할**: TextSplitterProcessor가 최적의 청크 크기로 분할
5. **이중 저장**: 
   - VectorStore에 벡터 임베딩 저장 (검색용)
   - RedisDocumentRepository에 메타데이터와 원본 저장 (출처 추적용)
6. **질문 처리**: 사용자 질문을 벡터로 변환하여 유사 문서 검색
7. **답변 생성**: 검색된 문서를 바탕으로 LLM이 답변 생성
8. **출처 매핑**: 참조 번호 기반으로 정확한 출처 정보 제공

## 🐛 문제 해결

### 출처가 안 나올 때
- Redis 실행 여부 확인
- 문서 로드 상태 확인 (/api/rag/status)

### 검색 결과가 없을 때
- 유사도 임계값 확인
- 임베딩 모델 정상 동작 확인

## 🔮 개선 사항

### 현재 구현된 기능
- ✅ 출처 불일치 문제 해결 (Top-K 한계 극복)
- ✅ Chunk 기반 문맥 유실 문제 해결
- ✅ 검색 정확도 및 응답 안정성 개선
- ✅ Redis 의존 구조 → 추상화 기반 구조로 개선
- ✅ 벡터 DB 교체 가능 구조 확보

---

## 📝 개발 일지

### 블로그 포스팅
<!-- TODO: 개발 블로그 링크 추가 예정 -->
- [Spring AI 와 RAG](https://velog.io/@wcw7373/03130603)
- [청킹(Chunking)과 벡터 데이터 최적화](https://velog.io/@wcw7373/03150351)
- [Redis 에서 Vector DB 사용기](https://velog.io/@wcw7373/03161143)
- [Redis 직접 제어의 문제](https://velog.io/@wcw7373/03161226)
- [HierarchicalParser 사용법](https://velog.io/@wcw7373/03160155)
- [RAG 검색의 출처 표기 문제](https://velog.io/@wcw7373/03171240)

