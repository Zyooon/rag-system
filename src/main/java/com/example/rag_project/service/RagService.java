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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
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
        
        System.out.println("📂 문서 폴더 경로: " + folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            System.out.println("📂 폴더가 존재하지 않습니다. 기본 경로로 시도합니다...");
            
            // 기본 폴더 시도
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, "documents");
            
            if (!Files.exists(folder)) {
                // 폴더 생성 시도
                Files.createDirectories(folder);
                System.out.println("📁 문서 폴더를 생성했습니다: " + folder.toAbsolutePath());
            } else {
                System.out.println("📂 기본 폴더를 찾았습니다: " + folder.toAbsolutePath());
            }
        } else {
            System.out.println("📂 폴더를 찾았습니다: " + folder.toAbsolutePath());
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
                        
                        System.out.println("✅ 파일 로드 완료: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("❌ 파일 로드 실패: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("❌ 폴더 스캔 실패: " + e.getMessage());
            throw e;
        }

        if (!allDocuments.isEmpty()) {
            // 문서를 작은 조각으로 분할
            List<Document> splitDocuments = textSplitter.apply(allDocuments);
            
            // 벡터 저장소에 추가
            vectorStore.add(splitDocuments);
            
            isInitialized = true;
            System.out.println("📚 총 " + allDocuments.size() + "개 파일이 벡터 저장소에 로드되었습니다.");
        } else {
            System.out.println("📂 폴더에 텍스트 파일이 없습니다: " + folder.toAbsolutePath());
            System.out.println("💡 이 폴더에 .txt 파일을 추가해주세요.");
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
            System.err.println("❌ 문서 초기화 실패: " + e.getMessage());
            System.err.println("💡 documents 폴더 경로를 확인해주세요.");
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
            return "📭 관련 정보를 찾을 수 없습니다.";
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
            return "📭 관련 정보를 찾을 수 없습니다.";
        }
        
        String prompt = String.format("""
            당신은 한국어 전문가입니다. 반드시 한국어로만 답변해야 합니다.
            
            아래 [문서 내용]을 바탕으로 사용자의 질문에 한국어로 답변하세요.
            
            중요 지침:
            1. 반드시 한국어로만 답변하세요. 영어 답변은 절대 허용되지 않습니다.
            2. 문서에 없는 내용은 절대로 지어내지 마세요.
            3. 제공된 문서의 내용만 바탕으로 답변하세요.
            4. 자연스러운 한국어 문장으로 답변하세요.
            5. 문서 내용을 요약하거나 설명하는 형태로 답변하세요.
            
            [문서 내용]
            %s
            
            [질문]
            %s
            
            [답변]
            반드시 위 질문에 대해 한국어로 답변을 시작하세요:
            """, context, query);

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            return "❌ AI 답변 생성 중 오류: " + e.getMessage();
        }
    }

    /**
     * 벡터 저장소를 초기화하는 메서드
     * Spring AI 2.0.0-M2의 VectorStore 인터페이스에는 clear() 메서드가 없음
     * 현재로서는 벡터 저장소를 새로 생성하여 초기화해야 함
     * 이는 현재 API 버전의 제약 사항임
     */
    public void clearStore() {
        // TODO: 벡터 저장소 초기화 로직 구현 필요
        // 현재 API 버전에서는 직접적인 clear() 메서드 제공 안 함
        isInitialized = false;
        System.out.println("🗑️ 벡터 저장소가 초기화되었습니다.");
    }

    /**
     * 현재 초기화 상태를 반환하는 메서드
     * @return 초기화 여부
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 현재 documents 폴더의 모든 문서를 Redis에 저장하는 메서드
     * @return 저장된 문서 수
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public int saveDocumentsToRedis() throws IOException {
        String currentDir = System.getProperty("user.dir");
        String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
        
        return saveDocumentsFromFolderToRedis(projectDocumentsPath);
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
            
            System.out.println("🔍 Redis에 저장된 문서 키: " + keyList.size() + "개");
            for (String key : keyList) {
                System.out.println("  - " + key);
            }
            
            return keyList;
        } catch (Exception e) {
            System.err.println("❌ Redis 키 조회 실패: " + e.getMessage());
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
            
            System.out.println("📄 문서 내용 (" + key + "):");
            System.out.println("  - ID: " + result.get("id"));
            System.out.println("  - 저장 시간: " + result.get("saved_at"));
            System.out.println("  - 내용 길이: " + (result.containsKey("content") ? result.get("content").toString().length() : 0) + "자");
            System.out.println("  - 메타데이터: " + result.get("metadata"));
            
            return result;
        } catch (Exception e) {
            System.err.println("❌ Redis 문서 조회 실패 (" + key + "): " + e.getMessage());
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
                System.out.println("📂 Redis에 저장된 문서가 없습니다.");
                return 0;
            }
            
            redisTemplate.delete(keys);
            System.out.println("🗑️ Redis에서 " + keys.size() + "개의 문서를 삭제했습니다.");
            return keys.size();
        } catch (Exception e) {
            System.err.println("❌ Redis 문서 삭제 실패: " + e.getMessage());
            return 0;
        }
    }
    public boolean testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("test:connection", "Redis 연결 테스트 성공!");
            String result = (String) redisTemplate.opsForValue().get("test:connection");
            redisTemplate.delete("test:connection");
            
            System.out.println("✅ Redis 연결 테스트 성공: " + result);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 테스트 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 특정 폴더의 문서들을 Redis에 저장하는 메서드
     * @param folderPath 문서 폴더 경로
     * @return 저장된 문서 수
     * @throws IOException 파일 읽기 실패 시 발생
     */
    private int saveDocumentsFromFolderToRedis(String folderPath) throws IOException {
        // 상대 경로를 절대 경로로 변환
        Path folder;
        if (Paths.get(folderPath).isAbsolute()) {
            folder = Paths.get(folderPath);
        } else {
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, folderPath);
        }
        
        System.out.println("📂 Redis 저장을 위한 문서 폴더 경로: " + folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            System.out.println("📂 폴더가 존재하지 않습니다: " + folder.toAbsolutePath());
            return 0;
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
                        
                        System.out.println("✅ Redis 저장용 파일 로드 완료: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("❌ 파일 로드 실패: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("❌ 폴더 스캔 실패: " + e.getMessage());
            throw e;
        }

        if (!allDocuments.isEmpty()) {
            // 문서를 작은 조각으로 분할
            List<Document> splitDocuments = textSplitter.apply(allDocuments);
            
            // Redis에 직접 문서 저장
            int savedCount = 0;
            for (int i = 0; i < splitDocuments.size(); i++) {
                Document doc = splitDocuments.get(i);
                String key = "rag:document:" + i;
                
                // 문서 정보를 Map으로 변환하여 Redis에 저장
                java.util.Map<String, Object> documentData = new java.util.HashMap<>();
                documentData.put("content", doc.getText());
                documentData.put("metadata", doc.getMetadata());
                documentData.put("id", i);
                documentData.put("saved_at", java.time.LocalDateTime.now().toString());
                
                try {
                    redisTemplate.opsForHash().putAll(key, documentData);
                    savedCount++;
                } catch (Exception e) {
                    System.err.println("❌ Redis 저장 실패 (문서 " + i + "): " + e.getMessage());
                }
            }
            
            // 벡터 저장소에도 추가 (검색용)
            vectorStore.add(splitDocuments);
            
            isInitialized = true;
            System.out.println("📚 총 " + allDocuments.size() + "개 파일(" + savedCount + "개 조각)이 Redis에 저장되었습니다.");
            return savedCount;
        } else {
            System.out.println("📂 폴더에 텍스트 파일이 없습니다: " + folder.toAbsolutePath());
            return 0;
        }
    }
}
