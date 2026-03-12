# RAG 문서 폴더

이 폴더는 RAG(Retrieval-Augmented Generation) 시스템에서 사용할 문서들을 저장하는 곳입니다.

## 사용 방법

1. 이 폴더에 텍스트 파일(.txt)을 추가하세요
2. 애플리케이션을 실행하면 자동으로 이 폴더의 모든 문서를 로드합니다
3. 질문 시 이 폴더의 문서 내용을 기반으로 답변이 생성됩니다

## 예시

- `company-policy.txt` - 회사 정책 문서
- `product-manual.txt` - 제품 설명서
- `faq.txt` - 자주 묻는 질문
- `meeting-notes.txt` - 회의록

## 설정

### 문서 폴더 경로
문서 폴더 경로는 `application.yml`에서 설정할 수 있습니다:

```yaml
rag:
  documents:
    folder: documents  # 기본값
```

환경 변수로도 설정 가능합니다:
```bash
export RAG_DOCUMENTS_FOLDER=/path/to/your/documents
```

### 유사도 임계값
검색된 문서의 유사도 임계값을 설정할 수 있습니다. 임계값보다 낮은 유사도의 문서는 필터링됩니다:

```yaml
rag:
  search:
    threshold: 0.5  # 기본값: 0.7
```

- **기본값**: 0.7
- **설정 범위**: 0.0 ~ 1.0
- **권장값**: 
  - 0.5 ~ 0.7: 일반적인 질문에 적합
  - 0.7 ~ 0.9: 더 정확한 답변이 필요할 때
  - 0.3 ~ 0.5: 더 넓은 범위의 검색이 필요할 때

## API 엔드포인트

### 문서 저장
- `POST /api/rag/save-to-redis` - documents 폴더의 모든 문서를 Redis에 저장

### 기존 기능
- `POST /api/rag/load` - 단일 텍스트 파일 로드
- `POST /api/rag/load-folder` - 폴더의 모든 문서 로드
- `POST /api/rag/initialize` - 기본 문서 폴더 자동 초기화
- `POST /api/rag/query` - 질문하기
- `GET /api/rag/status` - 시스템 상태 확인
- `DELETE /api/rag/clear` - 벡터 저장소 초기화

## Redis 저장 기능

### 특징
- **영속성**: 프로그램 재시작 후에도 문서 데이터 유지
- **자동 분할**: 문서를 자동으로 작은 조각으로 분리하여 저장
- **메타데이터**: 파일명, 경로, 저장 시간 등의 정보 포함
- **유사도 검색**: 벡터 기반 의미 검색 지원

### 사용 방법
```bash
# documents 폴더의 모든 문서를 Redis에 저장
curl -X POST http://localhost:8080/api/rag/save-to-redis
```

### 응답 예시
```json
{
  "success": true,
  "message": "총 45개의 문서 조각이 Redis에 저장되었습니다."
}
```

### 주의사항
- Redis 서버가 실행 중이어야 합니다 (localhost:6379)
- 현재는 SimpleVectorStore를 사용하며, 향후 RedisVectorStore로 업그레이드 예정
- 저장된 데이터는 프로그램 재시작 후에도 유지됩니다.
