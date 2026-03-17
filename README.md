# RAG 시스템 프로젝트

Spring Boot와 Spring AI를 기반으로 한 Retrieval-Augmented Generation (RAG) 시스템입니다. 로컬 LLM(Ollama)과 벡터 저장소(Redis)를 활용하여 문서 기반 질의응답 기능을 제공합니다.

## 🚀 주요 기능

- **문서 자동 처리**: 다양한 형식의 문서를 자동으로 파싱하고 벡터화
- **의미론적 검색**: 사용자 질문과 관련된 문서를 벡터 유사도로 검색
- **정확한 출처 추적**: 참조 번호 기반의 정확한 출처 정보 제공
- **다중 레벨 파싱**: 계층적 문서 구조 분석 (제목, 소제목, 목록 등)
- **Redis 영속성**: 벡터 저장소 메타데이터 유실 문제를 Redis로 보완

## 🛠️ 기술 스택

- **Java 17**: 최신 자바 버전
- **Spring Boot 4.0.3**: 웹 애플리케이션 프레임워크
- **Spring AI 2.0.0-M2**: AI 통합 프레임워크
- **Ollama**: 로컬 LLM (llama3, bge-m3 임베딩)
- **Redis Stack Server**: 벡터 저장소 및 캐시
- **Lombok**: 코드 생성 라이브러리
- **Gradle**: 빌드 도구

## 📁 프로젝트 구조

```
src/main/java/com/example/rag_project/
├── controller/          # REST API 컨트롤러
│   └── RagController.java
├── service/            # 비즈니스 로직
│   ├── RagService.java          # 메인 서비스
│   ├── SearchService.java        # 검색 및 답변 생성
│   ├── DocumentProcessingService.java  # 문서 처리
│   └── VectorStoreService.java   # 벡터 저장소 관리
├── dto/                # 데이터 전송 객체
│   ├── RagRequest.java
│   ├── RagResponse.java
│   └── SourceInfo.java
├── parser/             # 문서 파서
│   ├── HierarchicalParser.java
│   └── BulletParser.java
├── constants/          # 상수 정의
├── prompt/             # 프롬프트 템플릿
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

### 질문하기
```bash
POST /api/rag/ask
Content-Type: application/json

{
  "query": "네모난 사과의 특징은 무엇인가요?"
}
```

### 응답 예시
```json
{
  "success": true,
  "answer": "네모난 사과는 껍질이 보라색이며 맛은 민트초코 맛이 납니다. [1]",
  "sources": {
    "filename": "sample-doc.txt",
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

## 🔧 핵심 기능 설명

### 1. 출처 추적 시스템
- **참조 번호 기반 매칭**: LLM 답변의 `[1]`, `[2]` 참조를 실제 문서와 정확히 매핑
- **Redis 원본 비교**: 벡터 저장소 메타데이터 유실 시 Redis 원본과 내용 비교로 복원
- **다중 필터링**: 유사도 임계값, 중복 제거, 파일명 필터링

### 2. 문서 파싱
- **HierarchicalParser**: 제목 레벨별 분석 (H1, H2, H3)
- **BulletParser**: 목록 항목 및 구조화된 텍스트 처리
- **다양한 형식 지원**: 마크다운, 번호 목록, 대괄호 제목 등

### 3. 벡터 검색
- **의미론적 검색**: bge-m3 임베딩 모델 사용
- **유사도 필터링**: 설정 가능한 임계값 기반
- **효율적 저장**: Redis 벡터 저장소 활용

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

1. **문서 로딩**: 파일 시스템 또는 Redis에서 문서 로드
2. **파싱**: 문서 구조를 분석하여 청크로 분할
3. **벡터화**: 각 청크를 임베딩 벡터로 변환
4. **저장**: 벡터와 메타데이터를 Redis에 저장
5. **질문 처리**: 사용자 질문을 벡터로 변환하여 유사 문서 검색
6. **답변 생성**: 검색된 문서를 바탕으로 LLM이 답변 생성
7. **출처 매핑**: 참조 번호 기반으로 정확한 출처 정보 제공

## 🐛 문제 해결

### 출처 정보가 '알 수 없음'으로 나올 경우
1. Redis 서버가 실행 중인지 확인
2. 문서가 제대로 로드되었는지 확인
3. 로그를 통해 메타데이터 상태 확인

### 검색 결과가 없을 경우
1. 유사도 임계값 확인 (`rag.search.threshold`)
2. 문서가 벡터 저장소에 저장되었는지 확인
3. Ollama 모델이 제대로 로드되었는지 확인

## 📝 라이선스

이 프로젝트는 데모용으로 제작되었습니다.

## 🤝 기여

버그 리포트나 기능 요청은 이슈를 통해 제출해 주세요.
