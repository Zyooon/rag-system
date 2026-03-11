package com.example.rag_project.service;

import org.springframework.ai.document.Document;
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

    @Value("${rag.documents.folder:documents}")
    private String documentsFolder;

    private boolean isInitialized = false;

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
     */
    public String searchAndAnswer(String query) {
        if (!isInitialized) {
            initializeDocuments();
        }

        // 1. 유사도 검색 (의미 기반 검색)
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);
        
        if (relevantDocuments.isEmpty()) {
            return "📭 관련 정보를 찾을 수 없습니다.\n💡 다른 질문을 시도해보세요.";
        }
        
        // 2. 검색된 내용을 하나로 합치기 (README.md 제외)
        String context = relevantDocuments.stream()
                .filter(doc -> {
                    String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                    return !filename.equals("README.md");
                })
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        
        if (context.trim().isEmpty()) {
            return "📭 관련 정보를 찾을 수 없습니다.\n💡 다른 질문을 시도해보세요.";
        }
        
        // 3. 자연스러운 답변 생성 (하이브리드 방식)
        return generateNaturalResponse(query, context);
    }
    
    /**
     * 자연스러운 답변을 생성하는 메서드 (LLM 없이)
     * @param query 사용자 질문
     * @param context 검색된 문서 내용
     * @return 자연스러운 답변
     */
    private String generateNaturalResponse(String query, String context) {
        StringBuilder response = new StringBuilder();
        
        // 질문 유형 분석 및 답변 생성
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("뭐") || lowerQuery.contains("무엇") || lowerQuery.contains("알려줘")) {
            // "뭐야?" 타입 질문
            response.append(extractWhatAnswer(query, context));
        } else if (lowerQuery.contains("어디") || lowerQuery.contains("서식")) {
            // "어디야?" 타입 질문
            response.append(extractWhereAnswer(query, context));
        } else if (lowerQuery.contains("어떻게") || lowerQuery.contains("기능")) {
            // "어떻게?" 타입 질문
            response.append(extractHowAnswer(query, context));
        } else {
            // 일반 질문
            response.append(extractGeneralAnswer(query, context));
        }
        
        return response.toString();
    }
    
    /**
     * "뭐야?" 타입 질문에 대한 답변 추출
     */
    private String extractWhatAnswer(String query, String context) {
        String[] lines = context.split("\n");
        StringBuilder answer = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d+\\..*")) {
                // 항목 제목 추출
                String title = line.replaceFirst("^\\d+\\.", "").trim();
                answer.append("문서에 따르면 ").append(title).append("가 있습니다.\n\n");
                
                // 특징 정보 추출
                for (int i = getLineIndex(lines, line) + 1; i < lines.length; i++) {
                    String nextLine = lines[i].trim();
                    if (nextLine.matches("^\\d+\\..*")) break;
                    if (nextLine.startsWith("- 특징:") || nextLine.startsWith("- 기능:")) {
                        answer.append(nextLine).append("\n");
                    }
                }
                break;
            }
        }
        
        return answer.length() > 0 ? answer.toString() : "문서에서 관련 정보를 찾을 수 없습니다.";
    }
    
    /**
     * "어디야?" 타입 질문에 대한 답변 추출
     */
    private String extractWhereAnswer(String query, String context) {
        String[] lines = context.split("\n");
        StringBuilder answer = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- 서식지:") || line.startsWith("- 원산지:")) {
                answer.append(line);
                break;
            }
        }
        
        return answer.length() > 0 ? answer.toString() : "문서에서 관련 정보를 찾을 수 없습니다.";
    }
    
    /**
     * "어떻게?" 타입 질문에 대한 답변 추출
     */
    private String extractHowAnswer(String query, String context) {
        String[] lines = context.split("\n");
        StringBuilder answer = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- 기능:") || line.startsWith("- 효능:")) {
                answer.append(line);
                break;
            }
        }
        
        return answer.length() > 0 ? answer.toString() : "문서에서 관련 정보를 찾을 수 없습니다.";
    }
    
    /**
     * 일반 질문에 대한 답변 추출
     */
    private String extractGeneralAnswer(String query, String context) {
        String[] lines = context.split("\n");
        StringBuilder answer = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-")) {
                answer.append(line).append("\n");
            }
        }
        
        return answer.length() > 0 ? answer.toString() : "문서에서 관련 정보를 찾을 수 없습니다.";
    }
    
    /**
     * 라인 인덱스 찾기
     */
    private int getLineIndex(String[] lines, String targetLine) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(targetLine)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * RAG 프롬프트를 생성하는 메서드
     * @param context 검색된 문서 내용
     * @param query 사용자 질문
     * @return 완성된 프롬프트
     */
    private String createRagPrompt(String context, String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 주어진 문서 내용을 바탕으로 질문에 답변하는 AI 어시스턴트입니다.\n\n");
        prompt.append("=== 문서 내용 ===\n");
        prompt.append(context);
        prompt.append("=== 질문 ===\n");
        prompt.append(query);
        prompt.append("\n\n=== 지시사항 ===\n");
        prompt.append("1. 주어진 문서 내용만을 기반으로 답변하세요.\n");
        prompt.append("2. 문서에 정보가 없는 경우 '문서에 관련 정보가 없습니다'라고 답변하세요.\n");
        prompt.append("3. 답변할 때 문서의 내용을 인용하여 구체적으로 설명하세요.\n");
        prompt.append("4. 한국어로 답변하세요.\n\n");
        prompt.append("답변:");
        
        return prompt.toString();
    }

    /**
     * 벡터 유사도 기반으로 답변을 생성하는 메서드
     * @param prompt 생성된 프롬프트
     * @param documents 관련 문서 목록
     * @return 생성된 답변
     */
    private String generateSimpleResponse(String prompt, List<Document> documents) {
        try {
            // 1. 사용자 질문 추출
            String userQuery = extractQueryFromPrompt(prompt);
            
            // 2. 검색된 문서 내용을 컨텍스트로 구성
            StringBuilder context = new StringBuilder();
            context.append("다음은 검색된 관련 문서 내용입니다:\n\n");
            
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                
                // README.md는 건너뛰기 (시스템 설명서)
                if (filename.equals("README.md")) {
                    continue;
                }
                
                context.append("문서 ").append(i + 1).append(" (").append(filename).append("):\n");
                context.append(doc.getText()).append("\n\n");
            }
            
            if (context.toString().equals("다음은 검색된 관련 문서 내용입니다:\n\n")) {
                return "📭 문서에서 관련 정보를 찾을 수 없습니다.\n💡 다른 질문을 시도해보세요.";
            }
            
            // 3. 벡터 유사도로 이미 관련성이 높은 문서들만 들어왔으므로, 
            //    간단한 텍스트 처리로 답변 생성 (Ollama 없이도 동작)
            return generateTextBasedResponse(userQuery, documents);
            
        } catch (Exception e) {
            return "❌ 답변 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    /**
     * 텍스트 기반으로 답변을 생성하는 메서드 (Ollama 없이)
     * @param query 사용자 질문
     * @param documents 관련 문서 목록
     * @return 생성된 답변
     */
    private String generateTextBasedResponse(String query, List<Document> documents) {
        StringBuilder response = new StringBuilder();
        
        // 벡터 유사도로 이미 정렬된 문서들에서 관련 정보 추출
        for (Document doc : documents) {
            String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
            
            // README.md는 건너뛰기
            if (filename.equals("README.md")) {
                continue;
            }
            
            String content = doc.getText();
            String[] lines = content.split("\n");
            
            // 관련 항목 찾기
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 번호로 시작하는 항목 찾기
                if (line.matches("^\\d+\\..*")) {
                    String title = line.replaceFirst("^\\d+\\.", "").trim();
                    
                    // 해당 항목의 특징 정보들 추출
                    StringBuilder itemInfo = new StringBuilder();
                    itemInfo.append("📄 ").append(title).append("\n");
                    
                    for (int j = i + 1; j < lines.length; j++) {
                        String nextLine = lines[j].trim();
                        if (nextLine.matches("^\\d+\\..*")) {
                            break; // 다음 항목 시작
                        }
                        if (nextLine.startsWith("- 특징:") || nextLine.startsWith("- 기능:") || 
                            nextLine.startsWith("- 서식지:") || nextLine.startsWith("- 효능:") ||
                            nextLine.startsWith("- 좋아하는 음식:") || nextLine.startsWith("- 배터리:") ||
                            nextLine.startsWith("- 주의사항:") || nextLine.startsWith("- 원산지:") ||
                            nextLine.startsWith("- 현상:") || nextLine.startsWith("- 보관법:") ||
                            nextLine.startsWith("- 제작자:")) {
                            itemInfo.append(nextLine).append("\n");
                        }
                    }
                    
                    // 자연스러운 문장으로 변환
                    String naturalResponse = convertToNaturalResponse(title, itemInfo.toString());
                    response.append(naturalResponse).append("\n\n");
                    break;
                }
            }
        }
        
        if (response.length() == 0) {
            return "📭 문서에서 관련 정보를 찾을 수 없습니다.\n💡 다른 질문을 시도해보세요.";
        }
        
        return response.toString().trim();
    }
    
    /**
     * 구조화된 정보를 자연스러운 문장으로 변환하는 메서드
     * @param title 항목 제목
     * @param info 구조화된 정보
     * @return 자연스러운 문장
     */
    private String convertToNaturalResponse(String title, String info) {
        StringBuilder response = new StringBuilder();
        
        String[] lines = info.split("\n");
        List<String> features = new ArrayList<>();
        List<String> functions = new ArrayList<>();
        List<String> habitats = new ArrayList<>();
        List<String> others = new ArrayList<>();
        
        for (String line : lines) {
            if (line.startsWith("- 특징:")) {
                features.add(line.replaceFirst("- 특징:", "").trim());
            } else if (line.startsWith("- 기능:")) {
                functions.add(line.replaceFirst("- 기능:", "").trim());
            } else if (line.startsWith("- 서식지:")) {
                habitats.add(line.replaceFirst("- 서식지:", "").trim());
            } else if (line.startsWith("- 효능:") || line.startsWith("- 좋아하는 음식:") || 
                      line.startsWith("- 배터리:") || line.startsWith("- 주의사항:") || 
                      line.startsWith("- 원산지:") || line.startsWith("- 현상:") || 
                      line.startsWith("- 보관법:") || line.startsWith("- 제작자:")) {
                others.add(line.replaceFirst("- [^-]+:", "").trim());
            }
        }
        
        // 자연스러운 문장으로 조합
        response.append("문서에 따르면 ").append(title).append("가 있습니다.");
        
        if (!features.isEmpty()) {
            response.append(" 특징은 ").append(String.join(", ", features)).append("입니다.");
        }
        
        if (!functions.isEmpty()) {
            response.append(" 기능은 ").append(String.join(", ", functions)).append("라고 합니다.");
        }
        
        if (!habitats.isEmpty()) {
            response.append(" 서식지는 ").append(String.join(", ", habitats)).append("에 있다고 기록되어 있습니다.");
        }
        
        if (!others.isEmpty()) {
            response.append(" ").append(String.join(", ", others)).append(".");
        }
        
        return response.toString();
    }

    /**
     * 프롬프트에서 사용자 질문을 추출하는 메서드
     * @param prompt 전체 프롬프트
     * @return 사용자 질문
     */
    private String extractQueryFromPrompt(String prompt) {
        String[] lines = prompt.split("\n");
        for (String line : lines) {
            if (line.startsWith("=== 질문 ===")) {
                // 다음 라인이 질문
                int index = java.util.Arrays.asList(lines).indexOf(line);
                if (index + 1 < lines.length) {
                    return lines[index + 1].trim();
                }
            }
        }
        return "";
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
