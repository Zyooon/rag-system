package com.example.rag_project.service;

import com.example.rag_project.constants.ConfigConstants;
import com.example.rag_project.constants.ErrorConstants;
import com.example.rag_project.constants.MetadataConstants;
import com.example.rag_project.constants.MessageConstants;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.example.rag_project.parser.HierarchicalParser;
import com.example.rag_project.parser.BulletParser;
import com.example.rag_project.parser.SimpleLineParser;
import com.example.rag_project.splitter.TextSplitterFactory;
import com.example.rag_project.storage.RedisDocumentManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문서 처리 전담 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 문서 처리와 관련된 모든 작업을 담당합니다:</p>
 * <ul>
 *   <li>텍스트 파일 로딩 및 파싱</li>
 *   <li>문서 구조 분석 (계층적 파싱)</li>
 *   <li>문서 분할 (토큰 기반 청킹)</li>
 *   <li>Redis에 문서 저장 및 관리</li>
 *   <li>벡터 저장소에 문서 추가</li>
 * </ul>
 * 
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>파일 시스템에서 문서 읽기</li>
 *   <li>HierarchicalParser를 통한 문서 구조 분석</li>
 *   <li>TextSplitterFactory를 통한 문서 분할</li>
 *   <li>RedisDocumentManager를 통한 영속성 관리</li>
 * </ul>
 * 
 * <p><b>의존성:</b> ResourceLoader, VectorStore, RedisDocumentManager</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final ResourceLoader resourceLoader;
    private final VectorStore vectorStore;
    private final RedisDocumentManager redisDocumentManager;

    @Value("${" + ConfigConstants.CONFIG_DOCUMENTS_FOLDER + ":" + ConfigConstants.DEFAULT_DOCUMENTS_FOLDER + "}")
    private String documentsFolder;

    /**
     * 텍스트 파일을 읽어서 벡터 저장소에 로드하는 메서드
     */
    public void loadTextFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource(ConfigConstants.RESOURCE_CLASSPATH_PREFIX + filePath);
        TextReader textReader = new TextReader(resource);
        
        List<Document> documents = textReader.get();
        TokenTextSplitter textSplitter = TextSplitterFactory.createDefault();
        
        List<Document> splitDocuments = textSplitter.apply(documents);
        vectorStore.add(splitDocuments);
    }

    /**
     * 특정 폴더의 모든 텍스트 파일을 자동으로 로드하는 메서드
     */
    public void loadDocumentsFromFolder(String folderPath) throws IOException {
        Path folder = getFolderPath(folderPath);
        
        if (!Files.exists(folder)) {
            createDefaultFolder(folder);
        }

        List<Document> allDocuments = processFilesInFolder(folder);
        
        if (!allDocuments.isEmpty()) {
            List<Document> finalDocuments = createFinalDocuments(allDocuments);
            vectorStore.add(finalDocuments);
        }
    }

    /**
     * Redis에 저장된 모든 문서 내용을 조회하는 메서드
     */
    public List<Map<String, Object>> getAllRedisDocuments() {
        return redisDocumentManager.getAllDocuments();
    }

    /**
     * Redis에 저장된 모든 문서 키 목록 조회
     */
    public List<String> getAllRedisDocumentKeys() {
        return redisDocumentManager.getAllDocumentKeys();
    }

    /**
     * 특정 Redis 문서의 내용을 조회하는 메서드
     */
    public Map<String, Object> getRedisDocument(String key) {
        return redisDocumentManager.getDocument(key);
    }

    /**
     * 현재 documents 폴더의 모든 문서를 Redis에 저장하는 메서드
     */
    public Map<String, Object> saveDocumentsToRedis() throws IOException {
        String currentDir = System.getProperty(ConfigConstants.SYSTEM_USER_DIR);
        String projectDocumentsPath = Paths.get(currentDir, ConfigConstants.DOCUMENTS_FOLDER_NAME).toString();
        
        return saveDocumentsFromFolderToRedisWithDuplicateCheck(projectDocumentsPath);
    }

    /**
     * 특정 폴더의 문서들을 Redis에 저장하는 메서드 (중복 방지)
     */
    public Map<String, Object> saveDocumentsFromFolderToRedisWithDuplicateCheck(String folderPath) throws IOException {
        Path folder = getFolderPath(folderPath);
        
        if (!Files.exists(folder)) {
            return Map.of(
                MetadataConstants.MAP_KEY_SAVED_COUNT, 0,
                MetadataConstants.MAP_KEY_DUPLICATE_COUNT, 0,
                MetadataConstants.MAP_KEY_TOTAL_COUNT, 0,
                MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_FOLDER_NOT_EXISTS
            );
        }

        List<Document> allDocuments = loadDocumentsFromFolderSimple(folder);
        
        if (!allDocuments.isEmpty()) {
            List<Document> splitDocuments = TextSplitterFactory.createDefault().apply(allDocuments);
            Map<String, Object> saveResult = redisDocumentManager.saveDocuments(splitDocuments);
            
            if ((Integer) saveResult.get(MetadataConstants.MAP_KEY_SAVED_COUNT) > 0) {
                vectorStore.add(splitDocuments);
            }
            
            return createSaveResult(allDocuments, splitDocuments, saveResult);
        }
        
        return Map.of(
            MetadataConstants.MAP_KEY_SAVED_COUNT, 0,
            MetadataConstants.MAP_KEY_DUPLICATE_COUNT, 0,
            MetadataConstants.MAP_KEY_TOTAL_COUNT, 0,
            MetadataConstants.MAP_KEY_MESSAGE, MessageConstants.MSG_NO_TEXT_FILES
        );
    }

    // private helper methods
    private Path getFolderPath(String folderPath) {
        return Paths.get(folderPath).isAbsolute() ? 
            Paths.get(folderPath) : 
            Paths.get(System.getProperty(ConfigConstants.SYSTEM_USER_DIR), folderPath);
    }

    private void createDefaultFolder(Path folder) throws IOException {
        String currentDir = System.getProperty(ConfigConstants.SYSTEM_USER_DIR);
        folder = Paths.get(currentDir, ConfigConstants.DOCUMENTS_FOLDER_NAME);
        
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }
    }

    private List<Document> processFilesInFolder(Path folder) throws IOException {
        List<Document> allDocuments = new ArrayList<>();
        TokenTextSplitter textSplitter = TextSplitterFactory.createDefault();

        Files.list(folder)
            .filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                boolean isTextOrMd = fileName.endsWith(ConfigConstants.TXT_EXTENSION) || fileName.endsWith(ConfigConstants.MD_EXTENSION);
                boolean isReadme = fileName.equals("readme.md");
                return isTextOrMd && !isReadme;
            })
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    Map<String, Object> baseMetadata = new HashMap<>();
                    baseMetadata.put(MetadataConstants.METADATA_FILENAME, path.getFileName().toString());
                    baseMetadata.put(MetadataConstants.METADATA_FILEPATH, path.toString());
                    
                    HierarchicalParser hierarchicalParser = new HierarchicalParser();
                    List<Document> parsedDocuments = hierarchicalParser.parse(content, baseMetadata);
                    
                    // sample-odd.txt는 무조건 일반 불릿으로 처리
                    String fileName = path.getFileName().toString();
                    if (fileName.equals("sample-odd.txt") && parsedDocuments.size() > 5) {
                        log.debug("sample-odd.txt를 일반 불릿으로 처리: {}개 문서 -> BulletParser 재처리", parsedDocuments.size());
                        
                        // BulletParser로 재처리
                        BulletParser bulletParser = new BulletParser();
                        parsedDocuments = bulletParser.parse(content, baseMetadata);
                    }
                    
                    List<Document> finalDocuments = new ArrayList<>();
                    for (Document doc : parsedDocuments) {
                        if (doc.getText().length() > 800) {
                            List<Document> splitLongDocs = textSplitter.apply(List.of(doc));
                            for (Document splitDoc : splitLongDocs) {
                                splitDoc.getMetadata().putAll(doc.getMetadata());
                                finalDocuments.add(splitDoc);
                            }
                        } else {
                            finalDocuments.add(doc);
                        }
                    }
                    
                    allDocuments.addAll(finalDocuments);
                } catch (Exception e) {
                    log.error(ErrorConstants.LOG_DOCUMENT_PROCESS_FAILED, path.getFileName().toString(), e.getMessage());
                }
            });
            
        return allDocuments;
    }

    private List<Document> createFinalDocuments(List<Document> allDocuments) {
        List<Document> finalDocuments = new ArrayList<>();
        int globalChunkIndex = 0;
        
        for (Document originalDoc : allDocuments) {
            Map<String, Object> metadata = new HashMap<>(originalDoc.getMetadata());
            String uniqueId = originalDoc.getMetadata().getOrDefault(MetadataConstants.METADATA_FILENAME, MetadataConstants.UNKNOWN) + "_" + globalChunkIndex;
            metadata.put(MetadataConstants.METADATA_CHUNK_ID, uniqueId);
            metadata.put(MetadataConstants.METADATA_FILE_CHUNK_INDEX, globalChunkIndex);
            
            finalDocuments.add(new Document(originalDoc.getText(), metadata));
            globalChunkIndex++;
        }
        
        return finalDocuments;
    }

    private List<Document> loadDocumentsFromFolderSimple(Path folder) throws IOException {
        List<Document> allDocuments = new ArrayList<>();
        
        Files.list(folder)
            .filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                boolean isTextOrMd = fileName.endsWith(ConfigConstants.TXT_EXTENSION) || fileName.endsWith(ConfigConstants.MD_EXTENSION);
                boolean isReadme = fileName.equals("readme.md");
                return isTextOrMd && !isReadme;
            })
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    
                    // 기본 메타데이터 설정
                    Map<String, Object> baseMetadata = Map.of(
                        MetadataConstants.METADATA_FILENAME, path.getFileName().toString(),
                        MetadataConstants.METADATA_FILEPATH, path.toString(),
                        MetadataConstants.METADATA_SAVED_AT, java.time.LocalDateTime.now().toString()
                    );
                    
                    // 하이브리드 파싱: 여러 단계로 시도
                    HierarchicalParser hierarchicalParser = new HierarchicalParser();
                    List<Document> parsedDocuments = hierarchicalParser.parse(content, baseMetadata);
                    
                    if (parsedDocuments.isEmpty()) {
                        // 1단계: 불릿 기호 기반으로 다시 시도
                        BulletParser bulletParser = new BulletParser();
                        parsedDocuments = bulletParser.parse(content, baseMetadata);
                        log.info("BulletParser로 {}개 조각 분할: {}", parsedDocuments.size(), path.getFileName());
                    }
                    
                    if (parsedDocuments.isEmpty()) {
                        // 2단계: 줄바꿈(Double Newline) 기반으로 의미 단위 시도
                        SimpleLineParser simpleLineParser = new SimpleLineParser();
                        parsedDocuments = simpleLineParser.parse(content, baseMetadata);
                        log.info("SimpleLineParser로 {}개 조각 분할: {}", parsedDocuments.size(), path.getFileName());
                    }
                    
                    // 최종 Fallback: 정말 아무것도 안 되면 전체 저장
                    if (parsedDocuments.isEmpty()) {
                        Document document = new Document(content, baseMetadata);
                        allDocuments.add(document);
                        log.info("전체 문서로 저장: {}", path.getFileName());
                    } else {
                        allDocuments.addAll(parsedDocuments);
                        log.info("총 {}개 조각 저장: {}", parsedDocuments.size(), path.getFileName());
                    
                    // 디버그: 파싱된 문서 내용 출력 (sample-bullet.txt만)
                    if (path.getFileName().toString().equals("sample-bullet.txt")) {
                        log.info("=== sample-bullet.txt 파싱 결과 ===");
                        for (int i = 0; i < parsedDocuments.size(); i++) {
                            Document doc = parsedDocuments.get(i);
                            log.info("문서 {}: {}", i, doc.getText());
                            log.info("메타데이터 {}: {}", i, doc.getMetadata());
                        }
                    }
                    }
                    
                } catch (IOException e) {
                    log.error("Failed to read file: {}", path, e);
                }
            });
            
        return allDocuments;
    }

    private Map<String, Object> createSaveResult(List<Document> allDocuments, List<Document> splitDocuments, Map<String, Object> saveResult) {
        int savedCount = (Integer) saveResult.get(MetadataConstants.MAP_KEY_SAVED_COUNT);
        int duplicateCount = (Integer) saveResult.get(MetadataConstants.MAP_KEY_DUPLICATE_COUNT);
        
        String message = String.format(MessageConstants.MSG_FILES_PROCESSED, 
                                       allDocuments.size(), savedCount, duplicateCount);
        
        return Map.of(
            MetadataConstants.MAP_KEY_SAVED_COUNT, savedCount,
            MetadataConstants.MAP_KEY_DUPLICATE_COUNT, duplicateCount,
            MetadataConstants.MAP_KEY_TOTAL_COUNT, splitDocuments.size(),
            MetadataConstants.MAP_KEY_ORIGINAL_FILE_COUNT, allDocuments.size(),
            MetadataConstants.MAP_KEY_MESSAGE, message
        );
    }
    
    /**
     * 테스트용 문서 파싱 메서드
     */
    public List<Document> parseDocument(String content, String filename) {
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put(MetadataConstants.METADATA_FILENAME, filename);
        baseMetadata.put(MetadataConstants.METADATA_FILEPATH, "test://" + filename);
        
        // 하이브리드 파싱: 여러 단계로 시도
        HierarchicalParser hierarchicalParser = new HierarchicalParser();
        List<Document> parsedDocuments = hierarchicalParser.parse(content, baseMetadata);
        
        if (parsedDocuments.isEmpty()) {
            // 1단계: 불릿 기호 기반으로 다시 시도
            BulletParser bulletParser = new BulletParser();
            parsedDocuments = bulletParser.parse(content, baseMetadata);
            log.debug("BulletParser로 {}개 조각 분할: {}", parsedDocuments.size(), filename);
        }
        
        if (parsedDocuments.isEmpty()) {
            // 2단계: 줄바꿈(Double Newline) 기반으로 의미 단위 시도
            SimpleLineParser simpleLineParser = new SimpleLineParser();
            parsedDocuments = simpleLineParser.parse(content, baseMetadata);
            log.debug("SimpleLineParser로 {}개 조각 분할: {}", parsedDocuments.size(), filename);
        }
        
        // 최종 Fallback: 정말 아무것도 안 되면 전체 저장
        if (parsedDocuments.isEmpty()) {
            Document document = new Document(content, baseMetadata);
            parsedDocuments.add(document);
            log.debug("전체 문서로 저장: {}", filename);
        }
        
        log.debug("총 {}개 조각 파싱됨: {}", parsedDocuments.size(), filename);
        return parsedDocuments;
    }
}
