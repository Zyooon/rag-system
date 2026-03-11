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
