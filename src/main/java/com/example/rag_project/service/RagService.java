package com.example.rag_project.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.rag_project.dto.SourceInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${rag.documents.folder:documents}")
    private String documentsFolder;

    private boolean isInitialized = false;

    @Value("${rag.search.threshold:0.7}") // 설정이 없으면 0.7을 기본으로 사용
    private double similarityThreshold;

    /**
     * 텍스트 파일을 읽어서 벡터 저장소에 로드하는 메서드
     * @param filePath 클래스패스 기반의 파일 경로
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public void loadTextFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        TextReader textReader = new TextReader(resource);
        
        // 텍스트 파일에서 문서 읽기
        List<Document> documents = textReader.get();
        
        // 문서를 작은 조각으로 분할 (토큰 기반)
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);
        
        // 분할된 문서를 벡터 저장소에 추가
        vectorStore.add(splitDocuments);
        
        isInitialized = true;
    }

    /**
     * 특정 폴더의 모든 텍스트 파일을 자동으로 로드하는 메서드
     * @param folderPath 문서 폴더 경로
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public void loadDocumentsFromFolder(String folderPath) throws IOException {
        // 상대 경로를 절대 경로로 변환
        Path folder;
        if (Paths.get(folderPath).isAbsolute()) {
            folder = Paths.get(folderPath);
        } else {
            // 프로젝트 루트 기준 상대 경로
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, folderPath);
        }
        
        System.out.println("문서 폴더 경로: " + folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            System.out.println("폴더가 존재하지 않습니다. 기본 경로로 시도합니다...");
            
            // 기본 폴더 시도
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, "documents");
            
            if (!Files.exists(folder)) {
                // 폴더 생성 시도
                Files.createDirectories(folder);
                System.out.println("문서 폴더를 생성했습니다: " + folder.toAbsolutePath());
            } else {
                System.out.println("기본 폴더를 찾았습니다: " + folder.toAbsolutePath());
            }
        } else {
            System.out.println("폴더를 찾았습니다: " + folder.toAbsolutePath());
        }

        List<Document> allDocuments = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        try {
            // 폴더 내의 모든 .txt와 .md 파일 처리
            Files.list(folder)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".txt") || fileName.endsWith(".md");
                })
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Document document = new Document(content, 
                            java.util.Map.of("filename", path.getFileName().toString(), 
                                           "filepath", path.toString()));
                        allDocuments.add(document);
                        
                        System.out.println("파일 로드 완료: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("파일 로드 실패: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("폴더 스캔 실패: " + e.getMessage());
            throw e;
        }

        if (!allDocuments.isEmpty()) {
            List<Document> allSplitDocuments = new ArrayList<>();
            int globalChunkIndex = 0;
            
            // 각 파일별로 문서 분할 처리
            for (Document originalDoc : allDocuments) {
                System.out.println("파일 분할 시작: " + originalDoc.getMetadata().get("filename"));
                System.out.println("원본 내용 길이: " + originalDoc.getText().length());
                System.out.println("원본 내용 미리보기: " + originalDoc.getText().substring(0, Math.min(100, originalDoc.getText().length())) + "...");
                
                // 개별 문서 분할
                List<Document> singleDocList = new ArrayList<>();
                singleDocList.add(originalDoc);
                List<Document> splitDocuments = textSplitter.apply(singleDocList);
                
                System.out.println("분할된 조각 수: " + splitDocuments.size());
                
                // 각 분할된 문서 조각에 고유 ID 추가
                for (int i = 0; i < splitDocuments.size(); i++) {
                    Document doc = splitDocuments.get(i);
                    Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                    metadata.put("chunk_id", globalChunkIndex);
                    metadata.put("chunk_index", i);
                    metadata.put("file_chunk_index", i); // 파일 내 조각 인덱스
                    
                    // 새로운 Document 객체 생성 (metadata 업데이트)
                    Document updatedDoc = new Document(doc.getText(), metadata);
                    allSplitDocuments.add(updatedDoc);
                    
                    System.out.println("조각 " + globalChunkIndex + ": 길이=" + doc.getText().length() + 
                                     ", 내용=" + doc.getText().substring(0, Math.min(50, doc.getText().length())) + "...");
                    
                    globalChunkIndex++;
                }
            }
            
            // 벡터 저장소에 추가
            vectorStore.add(allSplitDocuments);
            
            isInitialized = true;
            System.out.println("총 " + allDocuments.size() + "개 파일이 벡터 저장소에 로드되었습니다.");
        } else {
            System.out.println("폴더에 텍스트 파일이 없습니다: " + folder.toAbsolutePath());
            System.out.println("이 폴더에 .txt 파일을 추가해주세요.");
        }
    }

    /**
     * 애플리케이션 시작 시 설정된 폴더의 문서들을 자동으로 로드하는 메서드
     */
    public void initializeDocuments() {
        try {
            // 프로젝트 루트 기준의 문서들만 로드
            String currentDir = System.getProperty("user.dir");
            String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
            loadDocumentsFromFolder(projectDocumentsPath);
            
        } catch (IOException e) {
            System.err.println("문서 초기화 실패: " + e.getMessage());
            System.err.println("documents 폴더 경로를 확인해주세요.");
        }
    }

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하는 메서드
     * @param query 사용자의 질문
     * @return RAG를 통해 생성된 답변
     * 
     * 처리 과정:
     * 1. 벡터 저장소에서 질문과 유사한 문서들을 검색
     * 2. 설정된 유사도 임계값(threshold)에 따라 문서 필터링
     * 3. 필터링된 문서들을 기반으로 LLM을 통해 자연스러운 한국어 답변 생성
     * 
     * 유사도 임계값은 application.yml에서 설정 가능:
     * rag.search.threshold (기본값: 0.7)
     */
    public String searchAndAnswer(String query) {
        if (!isInitialized) {
            initializeDocuments();
        }

        List<Document> relevantDocuments = vectorStore.similaritySearch(query);
        
        if (relevantDocuments.isEmpty()) {
            return "관련 정보를 찾을 수 없습니다.";
        }

        List<Document> filteredDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .collect(Collectors.toList());

        if (filteredDocuments.isEmpty()) {
            return "질문과 관련된 충분히 신뢰할 수 있는 정보를 찾을 수 없습니다.";
        }
        
        String context = relevantDocuments.stream()
                .filter(doc -> {
                    String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                    return !filename.equals("README.md");
                })
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        
        if (context.trim().isEmpty()) {
            return "관련 정보를 찾을 수 없습니다.";
        }
        
        String prompt = String.format("""
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

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            return "AI 답변 생성 중 오류: " + e.getMessage();
        }
    }

    /**
     * 사용자 질문에 대해 RAG를 통해 답변을 생성하고 출처 정보도 함께 반환하는 메서드
     * @param query 사용자의 질문
     * @return 답변과 출처 정보를 포함하는 Map
     */
    public Map<String, Object> searchAndAnswerWithSources(String query) {
        if (!isInitialized) {
            initializeDocuments();
        }

        System.out.println("=== 검색 요청: " + query + " ===");
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);
        System.out.println("찾은 문서 수: " + relevantDocuments.size());
        
        if (relevantDocuments.isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new ArrayList<SourceInfo>()
            );
        }

        // 모든 관련 문서 정보 출력 (디버깅)
        for (int i = 0; i < relevantDocuments.size(); i++) {
            Document doc = relevantDocuments.get(i);
            System.out.println("문서 " + i + ": " + 
                "파일명=" + doc.getMetadata().getOrDefault("filename", "알수없음") + 
                ", chunk_id=" + doc.getMetadata().get("chunk_id") + 
                ", 점수=" + doc.getScore() + 
                ", 내용길이=" + doc.getText().length() + 
                ", 내용미리보기=" + doc.getText().substring(0, Math.min(50, doc.getText().length())) + "...");
        }

        // 중복 제거를 위한 chunk_id 집합
        Set<Integer> seenChunkIds = new HashSet<>();
        List<Document> filteredDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .filter(doc -> {
                // chunk_id로 중복 확인
                Object chunkId = doc.getMetadata().get("chunk_id");
                if (chunkId != null) {
                    int id = ((Number) chunkId).intValue();
                    if (seenChunkIds.contains(id)) {
                        return false; // 중복된 문서 제외
                    }
                    seenChunkIds.add(id);
                    return true;
                }
                return true; // chunk_id가 없는 문서는 포함
            })
            .collect(Collectors.toList());

        if (filteredDocuments.isEmpty()) {
            return Map.of(
                "answer", "질문과 관련된 충분히 신뢰할 수 있는 정보를 찾을 수 없습니다.",
                "sources", new ArrayList<SourceInfo>()
            );
        }
        
        // 출처 정보 추출
        List<SourceInfo> sources = filteredDocuments.stream()
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                return !filename.equals("README.md");
            })
            .map(doc -> {
                SourceInfo source = SourceInfo.fromDocument(doc);
                System.out.println("출처 정보 - 파일명: " + source.getFilename() + 
                                 ", 조각 ID: " + source.getChunkId() +
                                 ", 유사도: " + source.getSimilarityScore() + 
                                 ", 내용 길이: " + (source.getContent() != null ? source.getContent().length() : 0));
                return source;
            })
            .collect(Collectors.toList());
        
        String context = filteredDocuments.stream()
                .filter(doc -> {
                    String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                    return !filename.equals("README.md");
                })
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        
        if (context.trim().isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new ArrayList<SourceInfo>()
            );
        }
        
        String prompt = String.format("""
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

        try {
            String answer = chatModel.call(prompt);
            return Map.of(
                "answer", answer,
                "sources", sources
            );
        } catch (Exception e) {
            return Map.of(
                "answer", "AI 답변 생성 중 오류: " + e.getMessage(),
                "sources", sources
            );
        }
    }

    /**
     * 벡터 저장소를 초기화하는 메서드
     * Spring AI 2.0.0-M2의 VectorStore 인터페이스에는 clear() 메서드가 없음
     * 현재로서는 벡터 저장소를 새로 생성하여 초기화해야 함
     * 이는 현재 API 버전의 제약 사항임
     */
    public void clearStore() {
        try {
            // 초기화 상태 변경
            isInitialized = false;
            
            // Redis에 저장된 문서도 삭제하여 깨끗한 상태로 만듦
            clearAllRedisDocuments();
            
            System.out.println("벡터 저장소와 Redis 문서가 초기화되었습니다.");
            System.out.println("다음 문서 로드 시 새로운 벡터 데이터가 생성됩니다.");
            
        } catch (Exception e) {
            System.err.println("벡터 저장소 초기화 실패: " + e.getMessage());
            throw new RuntimeException("벡터 저장소 초기화 중 오류 발생", e);
        }
    }

    /**
     * 현재 초기화 상태와 로드된 파일 목록을 반환하는 메서드
     * @return 초기화 상태 정보
     */
    public java.util.Map<String, Object> getStatusWithFiles() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("isInitialized", isInitialized());
        
        if (isInitialized()) {
            // Redis에 저장된 문서 키 목록 조회
            java.util.List<String> redisKeys = getAllRedisDocumentKeys();
            
            // 파일 목록 추출 (메타데이터에서 filename 추출)
            java.util.Set<String> loadedFiles = new java.util.HashSet<>();
            for (String key : redisKeys) {
                try {
                    java.util.Map<String, Object> doc = getRedisDocument(key);
                    if (doc.containsKey("metadata")) {
                        Object metadata = doc.get("metadata");
                        if (metadata instanceof java.util.Map) {
                            java.util.Map<?, ?> metaMap = (java.util.Map<?, ?>) metadata;
                            Object filename = metaMap.get("filename");
                            if (filename != null) {
                                loadedFiles.add(filename.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    // 무시하고 계속 진행
                }
            }
            
            status.put("loadedFiles", new java.util.ArrayList<>(loadedFiles));
            status.put("documentCount", redisKeys.size());
            status.put("message", "문서가 로드되어 있습니다.");
        } else {
            status.put("loadedFiles", new java.util.ArrayList<>());
            status.put("documentCount", 0);
            status.put("message", "문서가 로드되지 않았습니다.");
        }
        
        return status;
    }

    /**
     * 현재 초기화 상태를 반환하는 메서드
     * @return 초기화 여부
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 현재 documents 폴더의 모든 문서를 Redis에 저장하는 메서드 (중복 방지)
     * @return 저장 결과 정보
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public java.util.Map<String, Object> saveDocumentsToRedis() throws IOException {
        String currentDir = System.getProperty("user.dir");
        String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
        
        return saveDocumentsFromFolderToRedisWithDuplicateCheck(projectDocumentsPath);
    }

    /**
     * Redis에 저장된 모든 문서 키를 조회하는 메서드
     * @return 저장된 문서 키 목록
     */
    public java.util.List<String> getAllRedisDocumentKeys() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("rag:document:*");
            java.util.List<String> keyList = new java.util.ArrayList<>(keys);
            java.util.Collections.sort(keyList);
            
            System.out.println("Redis에 저장된 문서 키: " + keyList.size() + "개");
            for (String key : keyList) {
                System.out.println("  - " + key);
            }
            
            return keyList;
        } catch (Exception e) {
            System.err.println("Redis 키 조회 실패: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 특정 Redis 문서의 내용을 조회하는 메서드
     * @param key 문서 키
     * @return 문서 내용
     */
    public java.util.Map<String, Object> getRedisDocument(String key) {
        try {
            java.util.Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            
            for (java.util.Map.Entry<Object, Object> entry : data.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            
            System.out.println("문서 내용 (" + key + "):");
            System.out.println("  - ID: " + result.get("id"));
            System.out.println("  - 저장 시간: " + result.get("saved_at"));
            System.out.println("  - 내용 길이: " + (result.containsKey("content") ? result.get("content").toString().length() : 0) + "자");
            System.out.println("  - 메타데이터: " + result.get("metadata"));
            
            return result;
        } catch (Exception e) {
            System.err.println("Redis 문서 조회 실패 (" + key + "): " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    /**
     * Redis에 저장된 모든 문서 내용을 조회하는 메서드
     * @return 모든 문서 내용
     */
    public java.util.List<java.util.Map<String, Object>> getAllRedisDocuments() {
        java.util.List<String> keys = getAllRedisDocumentKeys();
        java.util.List<java.util.Map<String, Object>> documents = new java.util.ArrayList<>();
        
        for (String key : keys) {
            java.util.Map<String, Object> doc = getRedisDocument(key);
            if (!doc.isEmpty()) {
                documents.add(doc);
            }
        }
        
        System.out.println("📚 총 " + documents.size() + "개의 문서를 Redis에서 조회했습니다.");
        return documents;
    }

    /**
     * Redis에 저장된 모든 문서를 삭제하는 메서드
     * @return 삭제된 문서 수
     */
    public int clearAllRedisDocuments() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("rag:document:*");
            if (keys.isEmpty()) {
                System.out.println("Redis에 저장된 문서가 없습니다.");
                return 0;
            }
            
            redisTemplate.delete(keys);
            System.out.println("Redis에서 " + keys.size() + "개의 문서를 삭제했습니다.");
            return keys.size();
        } catch (Exception e) {
            System.err.println("Redis 문서 삭제 실패: " + e.getMessage());
            return 0;
        }
    }
    public boolean testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("test:connection", "Redis 연결 테스트 성공!");
            String result = (String) redisTemplate.opsForValue().get("test:connection");
            redisTemplate.delete("test:connection");
            
            System.out.println("Redis 연결 테스트 성공: " + result);
            return true;
        } catch (Exception e) {
            System.err.println("Redis 연결 테스트 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * Redis에 저장된 문서들을 벡터 저장소에 로드하는 메서드
     */
    public void loadDocumentsFromRedis() {
        try {
            List<Map<String, Object>> redisDocuments = getAllRedisDocuments();
            
            if (redisDocuments.isEmpty()) {
                System.out.println("Redis에 저장된 문서가 없습니다.");
                return;
            }
            
            List<Document> documents = new ArrayList<>();
            
            for (Map<String, Object> redisDoc : redisDocuments) {
                String content = (String) redisDoc.get("content");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) redisDoc.get("metadata");
                
                if (content != null && !content.trim().isEmpty()) {
                    Document document = new Document(content, metadata);
                    documents.add(document);
                }
            }
            
            if (!documents.isEmpty()) {
            List<Document> allSplitDocuments = new ArrayList<>();
            int globalChunkIndex = 0;
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            
            // 각 파일별로 문서 분할 처리
            for (Document originalDoc : documents) {
                System.out.println("Redis 파일 분할 시작: " + originalDoc.getMetadata().get("filename"));
                System.out.println("원본 내용 길이: " + originalDoc.getText().length());
                
                // 개별 문서 분할
                List<Document> singleDocList = new ArrayList<>();
                singleDocList.add(originalDoc);
                List<Document> splitDocuments = textSplitter.apply(singleDocList);
                
                // 각 분할된 문서 조각에 고유 ID 추가
                for (int i = 0; i < splitDocuments.size(); i++) {
                    Document doc = splitDocuments.get(i);
                    Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                    metadata.put("chunk_id", globalChunkIndex);
                    metadata.put("chunk_index", i);
                    metadata.put("file_chunk_index", i);
                    
                    // 새로운 Document 객체 생성 (metadata 업데이트)
                    Document updatedDoc = new Document(doc.getText(), metadata);
                    allSplitDocuments.add(updatedDoc);
                    
                    globalChunkIndex++;
                }
            }
            
            // 벡터 저장소에 추가
            vectorStore.add(allSplitDocuments);
                
                isInitialized = true;
                System.out.println("Redis에서 " + documents.size() + "개 문서를 벡터 저장소에 로드했습니다.");
            }
            
        } catch (Exception e) {
            System.err.println("Redis 문서 로드 실패: " + e.getMessage());
            throw new RuntimeException("Redis 문서 로드 중 오류 발생", e);
        }
    }

    /**
     * 특정 폴더의 문서들을 Redis에 저장하는 메서드 (중복 방지)
     * @param folderPath 문서 폴더 경로
     * @return 저장 결과 정보
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public java.util.Map<String, Object> saveDocumentsFromFolderToRedisWithDuplicateCheck(String folderPath) throws IOException {
        // 상대 경로를 절대 경로로 변환
        Path folder;
        if (Paths.get(folderPath).isAbsolute()) {
            folder = Paths.get(folderPath);
        } else {
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, folderPath);
        }
        
        System.out.println("Redis 저장을 위한 문서 폴더 경로: " + folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            System.out.println("폴더가 존재하지 않습니다: " + folder.toAbsolutePath());
            return java.util.Map.of(
                "savedCount", 0,
                "duplicateCount", 0,
                "totalCount", 0,
                "message", "폴더가 존재하지 않습니다."
            );
        }

        List<Document> allDocuments = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        try {
            // 폴더 내의 모든 .txt와 .md 파일 처리
            Files.list(folder)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".txt") || fileName.endsWith(".md");
                })
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Document document = new Document(content, 
                            java.util.Map.of("filename", path.getFileName().toString(), 
                                           "filepath", path.toString(),
                                           "saved_at", java.time.LocalDateTime.now().toString()));
                        allDocuments.add(document);
                        
                        System.out.println("Redis 저장용 파일 로드 완료: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("파일 로드 실패: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("폴더 스캔 실패: " + e.getMessage());
            throw e;
        }

        if (!allDocuments.isEmpty()) {
            // 문서를 작은 조각으로 분할
            List<Document> splitDocuments = textSplitter.apply(allDocuments);
            
            // 기존 Redis에 저장된 문서 키 확인
            java.util.Set<String> existingKeys = new java.util.HashSet<>(getAllRedisDocumentKeys());
            
            // Redis에 직접 문서 저장 (중복 방지)
            int savedCount = 0;
            int duplicateCount = 0;
            
            for (int i = 0; i < splitDocuments.size(); i++) {
                Document doc = splitDocuments.get(i);
                String key = "rag:document:" + i;
                
                // 중복 체크
                if (existingKeys.contains(key)) {
                    duplicateCount++;
                    System.out.println("중복 문서 건너뛰기: " + key);
                    continue;
                }
                
                // 문서 정보를 Map으로 변환하여 Redis에 저장
                java.util.Map<String, Object> documentData = new java.util.HashMap<>();
                documentData.put("content", doc.getText());
                documentData.put("metadata", doc.getMetadata());
                documentData.put("id", i);
                documentData.put("saved_at", java.time.LocalDateTime.now().toString());
                
                try {
                    redisTemplate.opsForHash().putAll(key, documentData);
                    savedCount++;
                    System.out.println("Redis 저장 완료: " + key);
                } catch (Exception e) {
                    System.err.println("Redis 저장 실패 (문서 " + i + "): " + e.getMessage());
                }
            }
            
            // 벡터 저장소에도 추가 (검색용)
            if (savedCount > 0) {
                vectorStore.add(splitDocuments);
                isInitialized = true;
            }
            
            String message = String.format("총 %d개 파일 처리 완료: %d개 저장, %d개 중복", 
                                           allDocuments.size(), savedCount, duplicateCount);
            System.out.println(message);
            
            return java.util.Map.of(
                "savedCount", savedCount,
                "duplicateCount", duplicateCount,
                "totalCount", splitDocuments.size(),
                "originalFileCount", allDocuments.size(),
                "message", message
            );
        } else {
            System.out.println("폴더에 텍스트 파일이 없습니다: " + folder.toAbsolutePath());
            return java.util.Map.of(
                "savedCount", 0,
                "duplicateCount", 0,
                "totalCount", 0,
                "message", "폴더에 텍스트 파일이 없습니다."
            );
        }
    }
}
