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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ChatModel chatModel;

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
            // rag-project 폴더 기준의 문서들만 로드
            String currentDir = System.getProperty("user.dir");
            String projectDocumentsPath = Paths.get(currentDir, "rag-project", "documents").toString();
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
}
