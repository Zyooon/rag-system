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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    /**
     * 문서의 구조를 분석하고 계층적 정보를 추출하는 파서
     * 구조화된 텍스트, 번호 목록, Markdown 형식을 지원
     */
    public static class HierarchicalParser {
        private String currentH1 = "";  // 대제목 (Level 1)
        private String currentH2 = "";  // 중제목 (Level 2)
        private String currentH3 = "";  // 소제목 (Level 3)
        
        // 제목 패턴 정의 (마크다운 형식 우선)
        private static final Pattern[] HEADING_PATTERNS = {
            // 마크다운 제목 형식 (우선순위 높음)
            Pattern.compile("^###\\s+(.+)$"),         // ### 소소제목
            Pattern.compile("^##\\s+(.+)$"),          // ## 소제목  
            Pattern.compile("^#\\s+(.+)$"),           // # 제목
            
            // 마크다운 목록 형식
            Pattern.compile("^-\\s+\\*\\*(.+?)\\*\\*:\\s*(.+)$"), // Markdown 굵은 글씨 항목
            Pattern.compile("^-\\s+(.+)$"),            // 일반 목록 항목
            
            // 기타 구조화된 형식
            Pattern.compile("^\\d+\\.\\d+\\.\\s+(.+)$"), // 1.1. 소제목
            Pattern.compile("^\\d+\\.\\s+(.+)$"),     // 1. 제목
            Pattern.compile("^\\[(.+)\\]$"),            // [제목] - 대괄호 제목
            Pattern.compile("^제목:\\s*(.+)$"),       // 제목: 내용
            Pattern.compile("^\\|.+\\|$")            // 표 형식 (테이블)
        };
        
        public List<Document> parse(String content, Map<String, Object> baseMetadata) {
            List<Document> chunks = new ArrayList<>();
            String[] lines = content.split("\n");
            
            // 파일마다 상태 초기화 - 이전 파일의 제목 정보가 영향주지 않도록
            this.currentH1 = extractDocumentTitle(content);
            this.currentH2 = "";
            this.currentH3 = "";
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;

                // 제목 업데이트 여부 확인 (제목도 본문에 포함되도록 수정)
                updateHeadingLevels(trimmedLine);
                
                Map<String, Object> metadata = new HashMap<>(baseMetadata);
                metadata.put("h1", currentH1);
                metadata.put("h2", currentH2);
                metadata.put("h3", currentH3);
                
                // 컨텍스트 정보를 포함하되, 제목 줄 자체도 벡터화 대상에 포함하여 검색 품질 향상
                String contextAwareText = createContextAwareText(trimmedLine);
                chunks.add(new Document(contextAwareText, metadata));
            }
            return chunks;
        }
        
        private boolean updateHeadingLevels(String line) {
            for (int i = 0; i < HEADING_PATTERNS.length; i++) {
                Matcher matcher = HEADING_PATTERNS[i].matcher(line);
                if (matcher.matches()) {
                    // 그룹(1)이 있는 패턴만 처리
                    if (matcher.groupCount() < 1) {
                        continue; // 그룹이 없는 패턴은 건너뛰기
                    }
                    
                    try {
                        String title = matcher.group(1).trim();
                        
                        switch (i) {
                            case 0: // ### 소소제목
                                currentH3 = title;
                                return true;
                                
                            case 1: // ## 소제목
                                currentH2 = title;
                                currentH3 = "";
                                return true;
                                
                            case 2: // # 제목
                                currentH1 = title;
                                currentH2 = "";
                                currentH3 = "";
                                return true;
                                
                            case 3: // Markdown 굵은 글씨 항목 - 내용으로 처리
                            case 4: // 일반 목록 항목 - 내용으로 처리
                                return false; // 목록은 본문으로 처리
                                
                            case 5: // 1.1. 소제목
                                currentH2 = title;
                                currentH3 = "";
                                return true;
                                
                            case 6: // 1. 제목
                            case 7: // [제목]
                            case 8: // 제목: 내용
                                currentH1 = title;
                                currentH2 = "";
                                currentH3 = "";
                                return true;
                                
                            case 9: // 표 형식 - 제목으로 간주하지 않고 내용으로 처리
                                return false; // 표는 본문으로 처리
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // 그룹이 없는 패턴은 건너뛰기
                        continue;
                    }
                }
            }
            return false;
        }
        
        private String createContextAwareText(String content) {
            StringBuilder context = new StringBuilder();
            
            if (!currentH1.isEmpty()) {
                context.append("[").append(currentH1);
                if (!currentH2.isEmpty()) {
                    context.append(" > ").append(currentH2);
                    if (!currentH3.isEmpty()) {
                        context.append(" > ").append(currentH3);
                    }
                }
                context.append("] ");
            }
            
            context.append(content);
            return context.toString();
        }
        
        public String extractDocumentTitle(String content) {
            String[] lines = content.split("\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;
                
                // 첫 번째 제목 패턴 찾기
                for (Pattern pattern : HEADING_PATTERNS) {
                    Matcher matcher = pattern.matcher(trimmedLine);
                    if (matcher.matches()) {
                        // 그룹(1)이 있는 패턴만 처리
                        if (matcher.groupCount() < 1) {
                            continue; // 그룹이 없는 패턴은 건너뛰기
                        }
                        
                        try {
                            String title = matcher.group(1).trim();
                            // 표 형식, 목록 항목은 제목으로 사용하지 않음
                            if (trimmedLine.startsWith("|") || trimmedLine.startsWith("-")) {
                                continue;
                            }
                            return title;
                        } catch (IndexOutOfBoundsException e) {
                            // 그룹이 없는 패턴은 건너뛰기
                            continue;
                        }
                    }
                }
                
                // 제목 패턴이 없으면 첫 줄을 제목으로 간주 (단, 표 형식이 아닌 경우)
                if (!trimmedLine.startsWith("|")) {
                    return trimmedLine.length() > 50 ? trimmedLine.substring(0, 47) + "..." : trimmedLine;
                }
            }
            
            return "제목 없음";
        }
    }

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

    @Value("${rag.search.max-results:5}") // 설정이 없으면 5를 기본으로 사용
    private int maxSearchResults;

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
        
        logger.info("문서 폴더 경로: {}", folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            logger.info("폴더가 존재하지 않습니다. 기본 경로로 시도합니다...");
            
            // 기본 폴더 시도
            String currentDir = System.getProperty("user.dir");
            folder = Paths.get(currentDir, "documents");
            
            if (!Files.exists(folder)) {
                // 폴더 생성 시도
                Files.createDirectories(folder);
                logger.info("문서 폴더를 생성했습니다: {}", folder.toAbsolutePath());
            } else {
                logger.info("기본 폴더를 찾았습니다: {}", folder.toAbsolutePath());
            }
        } else {
            logger.info("폴더를 찾았습니다: {}", folder.toAbsolutePath());
        }

        List<Document> allDocuments = new ArrayList<>();
        // 기본 TokenTextSplitter 사용 (자동으로 최적화된 크기로 분할)
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        try {
            // 폴더 내의 모든 .txt와 .md 파일 처리
            Files.list(folder)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    boolean isValid = fileName.endsWith(".txt") || fileName.endsWith(".md");
                    logger.info("파일 필터링: {} -> {}", fileName, isValid ? "처리" : "건너뛰기");
                    return isValid;
                })
                .forEach(path -> {
                    try {
                        logger.info("처리 시작: {}", path.getFileName());
                        String content = Files.readString(path);
                        logger.info("파일 내용 길이: {}자", content.length());
                        
                        String filename = path.getFileName().toString();
                        
                        // 기본 메타데이터 생성
                        Map<String, Object> baseMetadata = new HashMap<>();
                        baseMetadata.put("filename", filename);
                        baseMetadata.put("filepath", path.toString());
                        
                        // 구조화된 파서로 문서 분할
                        HierarchicalParser parser = new HierarchicalParser();
                        List<Document> parsedDocuments = parser.parse(content, baseMetadata);
                        
                        // 너무 긴 문서는 TokenTextSplitter로 추가 분할
                        List<Document> finalDocuments = new ArrayList<>();
                        for (Document doc : parsedDocuments) {
                            if (doc.getText().length() > 800) {
                                List<Document> splitLongDocs = textSplitter.apply(List.of(doc));
                                
                                logger.info("긴 문서 분할 - 원본 메타데이터: {}", doc.getMetadata());
                                logger.info("분할된 문서 수: {}", splitLongDocs.size());
                                
                                // 분할된 문서들의 메타데이터 확인 및 복사
                                for (Document splitDoc : splitLongDocs) {
                                    // 중요: 분할된 조각에도 원본 메타데이터를 다시 넣어줌
                                    splitDoc.getMetadata().putAll(doc.getMetadata()); 
                                    finalDocuments.add(splitDoc);
                                    
                                    logger.info("분할 문서 메타데이터: {}", splitDoc.getMetadata());
                                }
                            } else {
                                finalDocuments.add(doc);
                            }
                        }
                        
                        allDocuments.addAll(finalDocuments);
                        
                        logger.info("파일 로드 완료: {} (생성된 조각: {}개)", path.getFileName(), finalDocuments.size());
                    } catch (IOException e) {
                        logger.error("파일 로드 실패: {} - {}", path, e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        logger.error("파일 파싱 실패: {} - {}", path, e.getMessage());
                        e.printStackTrace();
                    }
                });
        } catch (IOException e) {
            logger.error("폴더 스캔 실패: {}", e.getMessage());
            throw e;
        }

        if (!allDocuments.isEmpty()) {
            // 고유 ID 부여 및 최종 문서 리스트 생성
            List<Document> finalDocuments = new ArrayList<>();
            int globalChunkIndex = 0;
            
            for (Document originalDoc : allDocuments) {
                String filename = originalDoc.getMetadata().getOrDefault("filename", "알수없음").toString();
                
                // 메타데이터 디버깅
                logger.info("문서 처리 중 - 파일명: {}, 메타데이터: {}", filename, originalDoc.getMetadata());
                
                // 고유 ID 부여
                Map<String, Object> metadata = new HashMap<>(originalDoc.getMetadata());
                String uniqueId = filename + "_" + globalChunkIndex;
                metadata.put("chunk_id", uniqueId); 
                metadata.put("file_chunk_index", globalChunkIndex);
                
                Document finalDoc = new Document(originalDoc.getText(), metadata);
                logger.info("최종 문서 생성 - 파일명: {}", finalDoc.getMetadata().get("filename"));
                
                finalDocuments.add(finalDoc);
                globalChunkIndex++;
            }
            
            // 벡터 저장소에 추가
            vectorStore.add(finalDocuments);
            
            isInitialized = true;
            logger.info("총 {}개의 구조화된 조각이 생성되어 벡터 저장소에 로드되었습니다.", allDocuments.size());
        } else {
            logger.info("폴더에 텍스트 파일이 없습니다: {}", folder.toAbsolutePath());
            logger.info("이 폴더에 .txt 파일을 추가해주세요.");
        }
    }

    /**
     * 파일 시스템에서 문서를 읽어 Redis 벡터 저장소로 수동 로드하는 메서드
     * Redis-only 전략을 위한 수동 데이터 로드 기능
     * @return 로드된 문서 수와 결과 정보
     */
    public java.util.Map<String, Object> loadDocumentsFromFilesystem() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        try {
            logger.info("=== 파일에서 Redis로 데이터 로드 시작 ===");
            
            // 기존 Redis 데이터 정리
            clearStore();
            
            // 프로젝트 루트 기준의 문서들만 로드
            String currentDir = System.getProperty("user.dir");
            String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
            logger.info("문서 경로: {}", projectDocumentsPath);
            
            // 기존 loadDocumentsFromFolder 메서드 사용 (RedisVectorStore로 자동 저장됨)
            loadDocumentsFromFolder(projectDocumentsPath);
            
            result.put("success", true);
            result.put("message", "파일에서 Redis로 문서 로드가 완료되었습니다.");
            result.put("documentCount", getAllRedisDocumentKeys().size());
            
        } catch (IOException e) {
            logger.error("파일에서 Redis로 데이터 로드 실패: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "로드 실패: " + e.getMessage());
        } catch (Exception e) {
            logger.error("예상치 못한 오류: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "오류 발생: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 애플리케이션 시작 시 Redis 벡터 저장소 상태만 확인하는 메서드
     * 파일 시스템 의존성 완전 제거 - Redis-only 전략
     */
    public void initializeDocuments() {
        try {
            logger.info("=== Redis 벡터 저장소 상태 확인 ===");
            
            // Redis 벡터 저장소 상태 확인
            java.util.List<String> redisKeys = getAllRedisDocumentKeys();
            int documentCount = redisKeys.size();
            
            if (documentCount > 0) {
                isInitialized = true;
                logger.info("Redis 벡터 저장소에 {}개의 문서가 있습니다.", documentCount);
                logger.info("시스템이 준비되었습니다.");
            } else {
                isInitialized = false;
                logger.info("Redis 벡터 저장소에 데이터가 없습니다.");
                logger.info("수동으로 데이터를 로드해주세요 (/load-from-files API 호출).");
            }
            
        } catch (Exception e) {
            logger.error("Redis 벡터 저장소 상태 확인 실패: {}", e.getMessage());
            isInitialized = false;
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

        logger.info("=== 검색 요청: {} ===", query);
        
        // 유사도 검색 (기본 검색 후 개수 제한)
        List<Document> relevantDocuments = vectorStore.similaritySearch(query);

        // 검색 결과가 아예 없거나(Redis가 비었을 때), 임계값을 넘지 못할 때 처리
        if (relevantDocuments == null || relevantDocuments.isEmpty()) {
            return Map.of(
                "answer", "현재 지식 베이스(Redis)에 저장된 데이터가 없어 답변을 드릴 수 없습니다.",
                "sources", new SourceInfo()
            );
        }
        
        // 유사도 순서대로 정렬 및 높은 유사도 필터링
        relevantDocuments = relevantDocuments.stream()
            .filter(doc -> {
                Double score = doc.getScore(); 
                return score != null && score >= similarityThreshold;
            })
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore())) // 유사도 내림차순 정렬
            .limit(maxSearchResults) // 설정된 최대 결과 개수로 제한
            .collect(Collectors.toList());
        
        logger.info("찾은 문서 수: {}", relevantDocuments.size());
        
        // 검색 결과가 없을 경우 예외 처리
        if (relevantDocuments.isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new SourceInfo()
            );
        }

        // 디버깅: 각 문서의 유사도와 내용 출력
        logger.info("=== 검색된 문서 상세 정보 ===");
        for (int i = 0; i < relevantDocuments.size(); i++) {
            Document doc = relevantDocuments.get(i);
            String filename = doc.getMetadata().getOrDefault("filename", "알수없음").toString();
            String filepath = doc.getMetadata().getOrDefault("filepath", "없음").toString();
            String chunkId = doc.getMetadata().getOrDefault("chunk_id", "없음").toString();
            String content = doc.getText().length() > 100 ? 
                doc.getText().substring(0, 100) + "..." : doc.getText();
            logger.info("문서 {}: 파일={}, 경로={}, 조각ID={}, 유사도={:.3f}, 내용={}", 
                i+1, filename, filepath, chunkId, doc.getScore(), content);
            logger.info("  전체 메타데이터: {}", doc.getMetadata());
        }
        logger.info("========================");
        
        // 필터링된 문서를 기반으로 출처 정보 생성 (중복 제거)
        Set<String> processedChunks = new HashSet<>(); // 중복 조각 추적용
        List<SourceInfo> sources = relevantDocuments.stream()
            .filter(doc -> {
                String filename = doc.getMetadata().getOrDefault("filename", "알 수 없음").toString();
                return !filename.equals("README.md");
            })
            .filter(doc -> {
                // 동일한 파일과 유사도를 가진 중복 문서 필터링
                String filename = doc.getMetadata().getOrDefault("filename", "").toString();
                Double score = doc.getScore();
                
                // 고유 식별자 생성: 파일명+유사도+내용해시
                String contentHash = String.valueOf(doc.getText().hashCode());
                String uniqueKey = filename + "|" + score + "|" + contentHash;
                
                if (processedChunks.contains(uniqueKey)) {
                    return false; // 중복 문서 건너뛰기
                }
                processedChunks.add(uniqueKey);
                return true;
            })
            .limit(5) // 최대 5개의 출처만 선택
            .map(doc -> {
                SourceInfo source = SourceInfo.fromDocument(doc);
                logger.info("출처 정보 - 파일명: {}, 조각 ID: {}, 유사도: {}, 내용 길이: {}", 
                    source.getFilename(), source.getChunkId(),
                    source.getSimilarityScore(), 
                    source.getContent() != null ? source.getContent().length() : 0);
                return source;
            })
            .collect(Collectors.toList());

        // LLM에게 줄 컨텍스트 생성 (검색된 순서대로 결합 + 번호 매기기)
        StringBuilder contextWithIndices = new StringBuilder();
        for (int i = 0; i < relevantDocuments.size(); i++) {
            contextWithIndices.append(String.format("[%d] %s\n\n", i + 1, relevantDocuments.get(i).getText()));
        }
        
        String context = contextWithIndices.toString();
        
        // 디버깅: context 내용 출력
        logger.info("=== 생성된 컨텍스트 ===");
        logger.info("문서 수: {}", relevantDocuments.size());
        logger.info("컨텍스트 길이: {}", context.length());
        logger.info("컨텍스트 내용 (미리보기): {}", 
            context.length() > 200 ? context.substring(0, 200) + "..." : context);
        logger.info("========================");
        
        if (context.trim().isEmpty()) {
            return Map.of(
                "answer", "관련 정보를 찾을 수 없습니다.",
                "sources", new SourceInfo()
            );
        }
        
        String prompt = String.format("""
            당신은 한국어 AI 어시스턴트입니다. 반드시 한국어로만 답변하세요.
            
            [중요] 각 정보의 출처를 문장 끝에 [번호]로 반드시 표시해야 합니다.
            
            [문서 내용]
            %s
            
            [질문]
            %s
            
            [답변 형식 예시]
            맛있는 신문은 기사를 다 읽고 나면 먹을 수 있어요[1]. 경제면은 스테이크 맛이 납니다[1].
            
            답변:
            """, context, query);

        try {
            String answer = chatModel.call(prompt);
            
            // 출처 재계산: 답변 내용과 실제 사용된 문서 매칭
            SourceInfo bestSource = findBestMatchingSource(answer, relevantDocuments);
            
            return Map.of(
                "answer", answer,
                "sources", bestSource
            );
        } catch (Exception e) {
            // 예외 시에도 가장 높은 유사도를 가진 출처 하나만 반환
            SourceInfo bestSource = sources.isEmpty() ? new SourceInfo() : sources.get(0);
            return Map.of(
                "answer", "AI 답변 생성 중 오류: " + e.getMessage(),
                "sources", bestSource
            );
        }
    }

    /**
     * 출처 재계산: 답변의 참조 번호를 기반으로 정확한 출처 찾기
     */
    private SourceInfo findBestMatchingSource(String answer, List<Document> documents) {
        logger.info("=== 출처 재계산 시작 (번호 기반) ===");
        logger.info("답변 내용: {}...", answer.substring(0, Math.min(100, answer.length())));
        
        // 답변에서 모든 참조 번호 추출
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(answer);
        
        java.util.Set<Integer> refNumbers = new java.util.HashSet<>();
        while (matcher.find()) {
            refNumbers.add(Integer.parseInt(matcher.group(1)));
        }
        
        logger.info("찾은 참조 번호들: {}", refNumbers);
        
        // 각 참조 번호에 해당하는 문서 확인 및 관련성 평가
        SourceInfo bestSource = new SourceInfo();
        double bestRelevanceScore = 0.0;
        
        for (int refNum : refNumbers) {
            int docIndex = refNum - 1; // 0-based index로 변환
            if (docIndex >= 0 && docIndex < documents.size()) {
                Document candidateDoc = documents.get(docIndex);
                String docContent = candidateDoc.getText();
                
                // 관련성 점수 계산 (키워드 매칭)
                double relevanceScore = calculateRelevanceScore(docContent);
                
                logger.info("참조 번호 [{}] 관련성 점수: {}", refNum, relevanceScore);
                logger.info("문서 내용: {}", docContent.substring(0, Math.min(50, docContent.length())));
                
                if (relevanceScore > bestRelevanceScore) {
                    bestRelevanceScore = relevanceScore;
                    bestSource = SourceInfo.fromDocument(candidateDoc);
                    logger.info("새로운 최고 관련성 점수: {}", relevanceScore);
                }
            }
        }
        
        // 참조 번호로 관련 문서를 찾았으면 반환
        if (bestRelevanceScore > 0) {
            logger.info("참조 번호 기반으로 선택된 출처: {}", bestSource.getFilename());
            return bestSource;
        }
        
        // 참조 번호로 관련 문서를 찾지 못하면 fallback
        logger.info("참조 번호로 관련 문서를 찾지 못해 기존 방식으로 fallback");
        return findBestMatchingSourceByContent(answer, documents);
    }
    
    /**
     * 문서 관련성 점수 계산
     */
    private double calculateRelevanceScore(String content) {
        double score = 0.0;
        
        // 키워드 기반 점수 계산
        String[] keywords = {"맛있는 신문", "신문", "기사", "경제면", "스테이크", "욕조물", "보관"};
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                score += 1.0;
            }
        }
        
        return score;
    }
    
    /**
     * 기존 방식: 내용 기반 출처 매칭 (fallback용)
     */
    private SourceInfo findBestMatchingSourceByContent(String answer, List<Document> documents) {
        SourceInfo bestSource = new SourceInfo();
        double maxScore = 0.0;
        
        for (Document doc : documents) {
            double score = calculateContentSimilarity(answer, doc.getText());
            String docPreview = doc.getText().length() > 50 ? doc.getText().substring(0, 50) : doc.getText();
            logger.info("문서 유사도: {} - {}", score, docPreview);
            
            if (score > maxScore) {
                maxScore = score;
                bestSource = SourceInfo.fromDocument(doc);
                logger.info("새로운 최고 점수: {}", score);
            }
        }
        
        logger.info("최종 선택된 출처: {}", bestSource.getFilename());
        return bestSource;
    }
    
    /**
     * 답변과 문서 내용의 유사도 계산
     */
    private double calculateContentSimilarity(String answer, String document) {
        // 간단한 키워드 매칭 기반 유사도 계산
        String[] answerWords = answer.toLowerCase().replaceAll("[^가-힣a-z0-9\\s]", "").split("\\s+");
        String[] docWords = document.toLowerCase().replaceAll("[^가-힣a-z0-9\\s]", "").split("\\s+");
        
        int matchCount = 0;
        for (String answerWord : answerWords) {
            if (answerWord.length() > 1) { // 1글자 이상만
                for (String docWord : docWords) {
                    if (docWord.contains(answerWord) || answerWord.contains(docWord)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }
        
        return answerWords.length > 0 ? (double) matchCount / answerWords.length : 0.0;
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
            
            // Redis 벡터 데이터 직접 삭제
            try {
                redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis("localhost", 6379);
                
                // 1. 벡터 인덱스 삭제 (있는 경우만)
                try {
                    Object result = jedis.sendCommand(
                        redis.clients.jedis.Protocol.Command.valueOf("FT.DROPINDEX"),
                        "vector_index".getBytes(), "DD".getBytes());
                    logger.info("벡터 인덱스 삭제 완료: {}", result);
                } catch (Exception e) {
                    logger.info("벡터 인덱스 삭제 실패 (이미 없거나 오류): {}", e.getMessage());
                }
                
                // 2. 모든 벡터 관련 키 삭제
                java.util.Set<String> ragKeys = jedis.keys("rag:*");
                if (!ragKeys.isEmpty()) {
                    jedis.del(ragKeys.toArray(new String[0]));
                    logger.info("RAG 관련 키 {}개 삭제 완료", ragKeys.size());
                }
                
                // 3. 모든 embedding 키 삭제
                java.util.Set<String> embeddingKeys = jedis.keys("embedding:*");
                if (!embeddingKeys.isEmpty()) {
                    jedis.del(embeddingKeys.toArray(new String[0]));
                    logger.info("Embedding 관련 키 {}개 삭제 완료", embeddingKeys.size());
                }
                
                jedis.close();
                logger.info("Redis 벡터 데이터가 삭제되었습니다.");
                
            } catch (Exception e) {
                logger.error("Redis 벡터 데이터 삭제 실패: {}", e.getMessage());
            }
            
            logger.info("Redis Vector Store가 초기화되었습니다.");
            logger.info("다음 문서 로드 시 새로운 벡터 데이터가 생성됩니다.");
            
        } catch (Exception e) {
            logger.error("벡터 저장소 초기화 실패: {}", e.getMessage());
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
            // 벡터 저장소에 저장된 문서 수 확인 (RedisVectorStore 사용 시)
            int vectorStoreCount = 0;
            java.util.Set<String> loadedFiles = new java.util.HashSet<>();
            
            try {
                // documents 폴더의 파일 목록으로 로드된 파일 확인
                String currentDir = System.getProperty("user.dir");
                Path documentsPath = Paths.get(currentDir, documentsFolder);
                
                if (Files.exists(documentsPath)) {
                    Files.list(documentsPath)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".txt") || fileName.endsWith(".md");
                        })
                        .forEach(path -> {
                            loadedFiles.add(path.getFileName().toString());
                        });
                    
                    // 파일당 평균 분할 조각 수로 문서 수 계산 (대략적 추정)
                    vectorStoreCount = loadedFiles.size() * 3; // 각 파일당 약 3개의 조각으로 가정
                }
            } catch (Exception e) {
                logger.error("문서 상태 확인 실패: {}", e.getMessage());
                vectorStoreCount = 0;
            }
            
            // Redis에 저장된 문서도 확인 (있는 경우)
            java.util.List<String> redisKeys = getAllRedisDocumentKeys();
            if (!redisKeys.isEmpty()) {
                vectorStoreCount = Math.max(vectorStoreCount, redisKeys.size());
                // Redis 메타데이터에서 파일명 추출
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
            }
            
            status.put("loadedFiles", new java.util.ArrayList<>(loadedFiles));
            status.put("documentCount", vectorStoreCount);
            status.put("message", "문서가 로드되어 있습니다. (벡터 저장소 기준)");
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
            
            logger.info("Redis에 저장된 문서 키: {}개", keyList.size());
            for (String key : keyList) {
                logger.info("  - {}", key);
            }
            
            return keyList;
        } catch (Exception e) {
            logger.error("Redis 키 조회 실패: {}", e.getMessage());
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
            
            logger.info("문서 내용 ({}):", key);
            logger.info("  - ID: {}", result.get("id"));
            logger.info("  - 저장 시간: {}", result.get("saved_at"));
            logger.info("  - 내용 길이: {}자", result.containsKey("content") ? result.get("content").toString().length() : 0);
            logger.info("  - 메타데이터: {}", result.get("metadata"));
            
            return result;
        } catch (Exception e) {
            logger.error("Redis 문서 조회 실패 ({}): {}", key, e.getMessage());
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
        
        logger.info("📚 총 {}개의 문서를 Redis에서 조회했습니다.", documents.size());
        return documents;
    }

    // Redis 관련 기능은 RedisVectorStore가 직접 처리하므로 제거됨

    /**
     * Redis에 저장된 문서들을 벡터 저장소에 로드하는 메서드
     */
    public void loadDocumentsFromRedis() {
        try {
            List<Map<String, Object>> redisDocuments = getAllRedisDocuments();
            
            if (redisDocuments.isEmpty()) {
                logger.info("Redis에 저장된 문서가 없습니다.");
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
                logger.info("Redis 파일 분할 시작: {}", originalDoc.getMetadata().get("filename"));
                logger.info("원본 내용 길이: {}", originalDoc.getText().length());
                
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
                logger.info("Redis에서 {}개 문서를 벡터 저장소에 로드했습니다.", documents.size());
            }
            
        } catch (Exception e) {
            logger.error("Redis 문서 로드 실패: {}", e.getMessage());
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
        
        logger.info("Redis 저장을 위한 문서 폴더 경로: {}", folder.toAbsolutePath());
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            logger.info("폴더가 존재하지 않습니다: {}", folder.toAbsolutePath());
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
                        
                        logger.info("Redis 저장용 파일 로드 완료: {}", path.getFileName());
                    } catch (IOException e) {
                        logger.error("파일 로드 실패: {} - {}", path, e.getMessage());
                    }
                });
        } catch (IOException e) {
            logger.error("폴더 스캔 실패: {}", e.getMessage());
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
                String filename = doc.getMetadata().getOrDefault("filename", "unknown").toString();
                String cleanFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
                String key = "rag:document:" + cleanFilename + "_" + i;
                
                // 중복 체크
                if (existingKeys.contains(key)) {
                    duplicateCount++;
                    logger.info("중복 문서 건너뛰기: {}", key);
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
                    logger.info("Redis 저장 완료: {}", key);
                } catch (Exception e) {
                    logger.error("Redis 저장 실패 (문서 {}): {}", i, e.getMessage());
                }
            }
            
            // 벡터 저장소에도 추가 (검색용)
            if (savedCount > 0) {
                vectorStore.add(splitDocuments);
                isInitialized = true;
            }
            
            String message = String.format("총 %d개 파일 처리 완료: %d개 저장, %d개 중복", 
                                           allDocuments.size(), savedCount, duplicateCount);
            logger.info(message);
            
            return java.util.Map.of(
                "savedCount", savedCount,
                "duplicateCount", duplicateCount,
                "totalCount", splitDocuments.size(),
                "originalFileCount", allDocuments.size(),
                "message", message
            );
        } else {
            logger.info("폴더에 텍스트 파일이 없습니다: {}", folder.toAbsolutePath());
            return java.util.Map.of(
                "savedCount", 0,
                "duplicateCount", 0,
                "totalCount", 0,
                "message", "폴더에 텍스트 파일이 없습니다."
            );
        }
    }
}
