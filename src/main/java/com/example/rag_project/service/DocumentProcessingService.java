package com.example.rag_project.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.example.rag_project.parser.HierarchicalParser;
import com.example.rag_project.splitter.TextSplitterFactory;
import com.example.rag_project.storage.RedisDocumentManager;

import lombok.RequiredArgsConstructor;

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
public class DocumentProcessingService {

    private final ResourceLoader resourceLoader;
    private final VectorStore vectorStore;
    private final RedisDocumentManager redisDocumentManager;

    @Value("${rag.documents.folder:documents}")
    private String documentsFolder;

    /**
     * 텍스트 파일을 읽어서 벡터 저장소에 로드하는 메서드
     */
    public void loadTextFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
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
        String currentDir = System.getProperty("user.dir");
        String projectDocumentsPath = Paths.get(currentDir, "documents").toString();
        
        return saveDocumentsFromFolderToRedisWithDuplicateCheck(projectDocumentsPath);
    }

    /**
     * 특정 폴더의 문서들을 Redis에 저장하는 메서드 (중복 방지)
     */
    public Map<String, Object> saveDocumentsFromFolderToRedisWithDuplicateCheck(String folderPath) throws IOException {
        Path folder = getFolderPath(folderPath);
        
        if (!Files.exists(folder)) {
            return Map.of(
                "savedCount", 0,
                "duplicateCount", 0,
                "totalCount", 0,
                "message", "폴더가 존재하지 않습니다."
            );
        }

        List<Document> allDocuments = loadDocumentsFromFolderSimple(folder);
        
        if (!allDocuments.isEmpty()) {
            List<Document> splitDocuments = TextSplitterFactory.createDefault().apply(allDocuments);
            Map<String, Object> saveResult = redisDocumentManager.saveDocuments(splitDocuments);
            
            if ((Integer) saveResult.get("savedCount") > 0) {
                vectorStore.add(splitDocuments);
            }
            
            return createSaveResult(allDocuments, splitDocuments, saveResult);
        }
        
        return Map.of(
            "savedCount", 0,
            "duplicateCount", 0,
            "totalCount", 0,
            "message", "폴더에 텍스트 파일이 없습니다."
        );
    }

    // private helper methods
    private Path getFolderPath(String folderPath) {
        return Paths.get(folderPath).isAbsolute() ? 
            Paths.get(folderPath) : 
            Paths.get(System.getProperty("user.dir"), folderPath);
    }

    private void createDefaultFolder(Path folder) throws IOException {
        String currentDir = System.getProperty("user.dir");
        folder = Paths.get(currentDir, "documents");
        
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
                return fileName.endsWith(".txt") || fileName.endsWith(".md");
            })
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    Map<String, Object> baseMetadata = new HashMap<>();
                    baseMetadata.put("filename", path.getFileName().toString());
                    baseMetadata.put("filepath", path.toString());
                    
                    HierarchicalParser parser = new HierarchicalParser();
                    List<Document> parsedDocuments = parser.parse(content, baseMetadata);
                    
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
                    // 에러 로깅
                }
            });
            
        return allDocuments;
    }

    private List<Document> createFinalDocuments(List<Document> allDocuments) {
        List<Document> finalDocuments = new ArrayList<>();
        int globalChunkIndex = 0;
        
        for (Document originalDoc : allDocuments) {
            Map<String, Object> metadata = new HashMap<>(originalDoc.getMetadata());
            String uniqueId = originalDoc.getMetadata().getOrDefault("filename", "알수없음") + "_" + globalChunkIndex;
            metadata.put("chunk_id", uniqueId);
            metadata.put("file_chunk_index", globalChunkIndex);
            
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
                return fileName.endsWith(".txt") || fileName.endsWith(".md");
            })
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    Document document = new Document(content, 
                        Map.of("filename", path.getFileName().toString(), 
                               "filepath", path.toString(),
                               "saved_at", java.time.LocalDateTime.now().toString()));
                    allDocuments.add(document);
                } catch (IOException e) {
                    // 에러 로깅
                }
            });
            
        return allDocuments;
    }

    private Map<String, Object> createSaveResult(List<Document> allDocuments, List<Document> splitDocuments, Map<String, Object> saveResult) {
        int savedCount = (Integer) saveResult.get("savedCount");
        int duplicateCount = (Integer) saveResult.get("duplicateCount");
        
        String message = String.format("총 %d개 파일 처리 완료: %d개 저장, %d개 중복", 
                                       allDocuments.size(), savedCount, duplicateCount);
        
        return Map.of(
            "savedCount", savedCount,
            "duplicateCount", duplicateCount,
            "totalCount", splitDocuments.size(),
            "originalFileCount", allDocuments.size(),
            "message", message
        );
    }
}
