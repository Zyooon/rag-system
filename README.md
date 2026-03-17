# RAG 시스템 프로젝트

Spring Boot와 Spring AI를 기반으로 한 Retrieval-Augmented Generation (RAG) 시스템입니다. 로컬 LLM(Ollama)과 벡터 저장소(Redis)를 활용하여 문서 기반 질의응답 기능을 제공합니다.

## 🚀 주요 기능

- **문서 자동 처리**: 다양한 형식의 문서를 자동으로 파싱하고 벡터화
- **의미론적 검색**: 사용자 질문과 관련된 문서를 벡터 유사도로 검색
- **정확한 출처 추적**: 참조 번호 기반의 정확한 출처 정보 제공
- **다중 레벨 파싱**: 계층적 문서 구조 분석 (제목, 소제목, 목록 등)
- **중복 문서 방지**: 고유 키 생성으로 중복 저장 방지
- **벡터-메타데이터 동기화**: VectorStore와 Redis의 이중 저장 구조

## 🛠️ 기술 스택

- **Java 17**: 최신 자바 버전
- **Spring Boot 4.0.3**: 웹 애플리케이션 프레임워크
- **Spring AI 2.0.0-M2**: AI 통합 프레임워크
- **Ollama**: 로컬 LLM (llama3, bge-m3 임베딩)
- **Redis Stack Server**: 벡터 저장소 및 메타데이터 저장
- **Lombok**: 코드 생성 라이브러리
- **Gradle**: 빌드 도구

## 📁 프로젝트 구조

```
src/main/java/com/example/rag_project/
├── controller/          # REST API 컨트롤러
│   ├── RagController.java      # 메인 RAG API
│   └── SearchController.java   # 검색 전용 API
├── service/            # 비즈니스 로직
│   ├── RagManagementService.java  # RAG 시스템 통합 관리
│   ├── SearchService.java        # 검색 및 답변 생성
│   ├── ParseManager.java         # 파서 관리자
│   └── FileManager.java          # 파일 관리
├── repository/         # 데이터 접근 계층
│   ├── RedisDocumentRepository.java  # 문서 저장소
│   └── RedisSearchRepository.java     # 검색 전용 저장소
├── parser/             # 문서 파서
│   ├── HierarchicalParser.java  # 계층적 구조 파싱
│   ├── BulletParser.java        # 목록 항목 파싱
│   ├── MarkdownParser.java      # 마크다운 파싱
│   └── SimpleLineParser.java    # 단순 라인 파싱
├── splitter/           # 텍스트 분할기
│   ├── TextSplitterProcessor.java  # 텍스트 분할 처리
│   ├── TextSplitterFactory.java    # 분할기 팩토리
│   └── TextSplitterConfig.java     # 분할 설정
├── dto/                # 데이터 전송 객체
│   ├── RagRequest.java
│   ├── RagResponse.java
│   └── SourceInfo.java
├── config/             # 설정 클래스
│   ├── RagConfig.java           # 메인 설정
│   ├── VectorStoreConfig.java   # 벡터 저장소 설정
│   └── RagAutoLoadConfig.java   # 자동 로드 설정
├── constants/          # 상수 정의
│   ├── CommonConstants.java     # 공통 상수
│   ├── ConfigConstants.java     # 설정 상수
│   └── MessageConstants.java    # 메시지 상수
├── exception/          # 예외 처리
│   ├── GlobalExceptionHandler.java
│   └── RagServiceException.java
├── prompt/             # 프롬프트 템플릿
│   └── PromptTemplate.java
└── RagProjectApplication.java
```

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

## 🔧 핵심 기능 설명

### 1. 이중 저장 아키텍처
- **VectorStore**: 벡터 임베딩 데이터 저장 (의미론적 검색용)
- **RedisDocumentRepository**: 메타데이터와 원본 텍스트 저장 (출처 추적용)
- **동기화**: 두 저장소 간 데이터 일관성 유지

### 2. 고급 문서 파싱
- **ParseManager**: 최적의 파서 자동 선택
- **HierarchicalParser**: 계층적 구조 분석 (H1, H2, H3)
- **BulletParser**: 목록 항목 및 구조화된 텍스트 처리
- **MarkdownParser**: 마크다운 형식 지원
- **SimpleLineParser**: 단순 텍스트 처리

### 3. 지능형 텍스트 분할
- **TextSplitterProcessor**: 의미 단위 텍스트 분할
- **TextSplitterFactory**: 문서 특성에 맞는 분할기 선택
- **최적 청킹**: 검색 효율성 극대화

### 4. 정확한 출처 추적
- **참조 번호 기반 매칭**: LLM 답변의 `[1]`, `[2]` 참조를 실제 문서와 정확히 매핑
- **Redis 원본 비교**: 벡터 저장소 메타데이터 유실 시 Redis 원본과 내용 비교로 복원
- **다중 필터링**: 유사도 임계값, 중복 제거, 파일명 필터링
- **고유 키 생성**: 중복 문서 방지 및 정확한 식별

### 5. 벡터 검색 최적화
- **의미론적 검색**: bge-m3 임베딩 모델 사용
- **유사도 필터링**: 설정 가능한 임계값 기반
- **효율적 저장**: Redis 벡터 저장소 활용
- **스코어링**: 정확한 유사도 계산 및 정렬

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

## 🧪 테스트

샘플 문서가 `documents/` 폴더에 포함되어 있습니다:

- `sample-odd.txt`: 번호 목록 형식의 이상한 것들 사전
- `sample-markdown.txt`: 마크다운 형식 문서
- `sample-table.txt`: 표 형식 문서
- `sample-mixed.txt`: 혼합 형식 문서
- 기타 다양한 형식의 테스트 문서들

## 🐛 문제 해결

### 출처 정보가 '알 수 없음'으로 나올 경우
1. Redis 서버가 실행 중인지 확인
2. 문서가 제대로 로드되었는지 확인 (`/api/rag/status`)
3. VectorStore와 Redis 데이터 동기화 상태 확인
4. 로그를 통해 메타데이터 상태 확인

### 검색 결과가 없을 경우
1. 유사도 임계값 확인 (`rag.search.threshold`)
2. 문서가 벡터 저장소에 저장되었는지 확인
3. Ollama 모델이 제대로 로드되었는지 확인
4. 문서 형식이 파서에서 지원되는지 확인

### 벡터 데이터가 저장되지 않을 경우
1. VectorStore 빈 주입 확인
2. Redis Stack Server 벡터 기능 활성화 확인
3. 임베딩 모델 (bge-m3) 동작 확인

## 📈 성능 최적화

### 문서 처리 최적화
- **중복 제거**: 고유 키 생성으로 불필요한 재처리 방지
- **분할 최적화**: 의미 단위 분할로 검색 정확도 향상
- **파서 자동 선택**: 문서 특성에 맞는 최적 파서 사용

### 검색 최적화
- **유사도 임계값**: 정확도와 속도의 균형 조절
- **최대 결과 수**: 응답 속도 최적화
- **캐싱**: 자주 검색되는 질문에 대한 캐시

## 🔮 개선 사항

### 현재 구현된 기능
- ✅ 이중 저장 아키텍처 (VectorStore + Redis)
- ✅ 고급 문서 파싱 및 분할
- ✅ 정확한 출처 추적 시스템
- ✅ 중복 문서 방지
- ✅ 다양한 문서 형식 지원
