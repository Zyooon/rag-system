package com.example.rag_project.service;

import com.example.rag_project.config.VectorStoreConfig;
import com.example.rag_project.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 파일 시스템 관리 전문 서비스
 * 
 * <p>이 클래스는 RAG 시스템의 <b>파일 시스템 작업</b>을 담당합니다:</p>
 * <ul>
 *   <li>📁 <b>폴더 관리</b> - 문서 폴더 생성 및 존재 확인</li>
 *   <li>📖 <b>파일 읽기</b> - 텍스트 및 마크다운 파일 읽기</li>
 *   <li>🔍 <b>파일 필터링</b> - 지원 파일 형식 필터링</li>
 *   <li>📊 <b>메타데이터 생성</b> - 파일 관련 메타데이터 생성</li>
 *   <li>🗂️ <b>경로 처리</b> - 절대/상대 경로 변환</li>
 * </ul>
 * 
 * <p><b>핵심 책임:</b></p>
 * <ul>
 *   <li><b>파일 시스템 접근</b>: 파일 읽기, 쓰기, 폴더 관리</li>
 *   <li><b>파일 필터링</b>: 지원 형식(.txt, .md) 필터링</li>
 *   <li><b>경로 관리</b>: 다양한 경로 형식 처리</li>
 *   <li><b>메타데이터</b>: 파일 정보 기반 메타데이터 생성</li>
 * </ul>
 * 
 * <p><b>지원 파일 형식:</b></p>
 * <ul>
 *   <li><b>.txt</b> - 일반 텍스트 파일</li>
 *   <li><b>.md</b> - 마크다운 파일</li>
 * </ul>
 * 
 * <p><b>의존성:</b> 없음 (순수 파일 시스템 작업)</p>
 * <p><b>출력물:</b> 파일 내용, 메타데이터, 파일 목록</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileManager {

    private final VectorStoreConfig vectorStoreConfig;

    /**
     * 폴더 경로를 Path 객체로 변환
     * 
     * @param folderPath 폴더 경로 (절대 또는 상대)
     * @return Path 객체
     */
    public Path getFolderPath(String folderPath) {
        return Paths.get(folderPath).isAbsolute() ? 
            Paths.get(folderPath) : 
            Paths.get(System.getProperty(CommonConstants.SYSTEM_USER_DIR), folderPath);
    }

    /**
     * 기본 문서 폴더 경로 반환
     * 
     * @return 기본 문서 폴더 Path
     */
    public Path getDefaultDocumentsPath() {
        String currentDir = System.getProperty(CommonConstants.SYSTEM_USER_DIR);
        return Paths.get(currentDir, vectorStoreConfig.getDocumentsFolder());
    }

    /**
     * 폴더가 존재하지 않으면 생성
     * 
     * @param folder 생성할 폴더 경로
     * @throws IOException 폴더 생성 실패 시
     */
    public void createFolderIfNotExists(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            log.info("폴더 생성 완료: {}", folder);
        }
    }

    /**
     * 기본 문서 폴더 생성
     * 
     * @throws IOException 폴더 생성 실패 시
     */
    public void createDefaultDocumentsFolder() throws IOException {
        Path defaultFolder = getDefaultDocumentsPath();
        createFolderIfNotExists(defaultFolder);
    }

    /**
     * 폴더 존재 여부 확인
     * 
     * @param folder 확인할 폴더 경로
     * @return 존재 여부
     */
    public boolean folderExists(Path folder) {
        return Files.exists(folder);
    }

    /**
     * 파일 내용 읽기
     * 
     * @param filePath 파일 경로
     * @return 파일 내용
     * @throws IOException 파일 읽기 실패 시
     */
    public String readFileContent(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    /**
     * 파일 메타데이터 생성
     * 
     * @param filePath 파일 경로
     * @return 파일 메타데이터 맵
     */
    public Map<String, Object> createFileMetadata(Path filePath) {
        return Map.of(
            CommonConstants.METADATA_KEY_FILENAME, filePath.getFileName().toString(),
            CommonConstants.METADATA_KEY_FILEPATH, filePath.toString(),
            CommonConstants.KEY_SAVED_AT, LocalDateTime.now().toString()
        );
    }

    /**
     * 지원하는 텍스트 파일인지 확인
     * 
     * @param fileName 파일명
     * @return 지원 여부
     */
    public boolean isSupportedTextFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(CommonConstants.TXT_EXTENSION) || 
               lowerFileName.endsWith(CommonConstants.MD_EXTENSION);
    }

    /**
     * 폴더 내의 지원 파일 목록 반환
     * 
     * @param folder 검색할 폴더
     * @return 지원 파일 Path 목록
     * @throws IOException 파일 목록 읽기 실패 시
     */
    public List<Path> getSupportedFilesInFolder(Path folder) throws IOException {
        List<Path> supportedFiles = new ArrayList<>();
        
        Files.list(folder)
            .filter(path -> isSupportedTextFile(path.getFileName().toString()))
            .forEach(supportedFiles::add);
        
        log.debug("지원 파일 {}개 발견: {}", supportedFiles.size(), folder);
        return supportedFiles;
    }

    /**
     * 폴더 내의 모든 지원 파일 내용과 메타데이터 읽기
     * 
     * @param folder 대상 폴더
     * @return 파일 정보 리스트 (내용, 메타데이터)
     * @throws IOException 파일 읽기 실패 시
     */
    public List<FileContent> readAllSupportedFiles(Path folder) throws IOException {
        List<FileContent> fileContents = new ArrayList<>();
        List<Path> supportedFiles = getSupportedFilesInFolder(folder);
        
        for (Path filePath : supportedFiles) {
            try {
                String content = readFileContent(filePath);
                Map<String, Object> metadata = createFileMetadata(filePath);
                fileContents.add(new FileContent(filePath, content, metadata));
                
                log.debug("파일 읽기 완료: {} (길이: {})", filePath.getFileName(), content.length());
            } catch (Exception e) {
                log.error("파일 읽기 실패: {} - {}", filePath.getFileName(), e.getMessage());
            }
        }
        
        log.info("총 {}개 파일 처리 완료", fileContents.size());
        return fileContents;
    }

    /**
     * 기본 문서 폴더의 모든 지원 파일 읽기
     * 
     * @return 파일 정보 리스트
     * @throws IOException 파일 읽기 실패 시
     */
    public List<FileContent> readAllDefaultDocuments() throws IOException {
        Path defaultFolder = getDefaultDocumentsPath();
        return readAllSupportedFiles(defaultFolder);
    }

    /**
     * 폴더 경로로 지원 파일 읽기 (문자열 경로)
     * 
     * @param folderPath 폴더 경로
     * @return 파일 정보 리스트
     * @throws IOException 파일 읽기 실패 시
     */
    public List<FileContent> readAllSupportedFiles(String folderPath) throws IOException {
        Path folder = getFolderPath(folderPath);
        return readAllSupportedFiles(folder);
    }

    /**
     * 폴더 존재 확인 및 필요시 생성
     * 
     * @param folderPath 폴더 경로
     * @return 폴더 존재 여부
     * @throws IOException 폴더 생성 실패 시
     */
    public boolean ensureFolderExists(String folderPath) throws IOException {
        Path folder = getFolderPath(folderPath);
        
        if (!folderExists(folder)) {
            if (folderPath.equals(vectorStoreConfig.getDocumentsFolder())) {
                createDefaultDocumentsFolder();
            } else {
                createFolderIfNotExists(folder);
            }
        }
        
        return folderExists(folder);
    }

    /**
     * 폴더 상태 확인 결과 생성
     * 
     * @param folderPath 폴더 경로
     * @return 폴더 상태 정보
     */
    public Map<String, Object> getFolderStatus(String folderPath) {
        Path folder = getFolderPath(folderPath);
        return getFolderStatus(folder);
    }

    /**
     * 파일 정보를 담는 내부 클래스
     */
    public static class FileContent {
        private final Path filePath;
        private final String content;
        private final Map<String, Object> metadata;

        public FileContent(Path filePath, String content, Map<String, Object> metadata) {
            this.filePath = filePath;
            this.content = content;
            this.metadata = metadata;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getContent() {
            return content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String getFilename() {
            return filePath.getFileName().toString();
        }
    }

    /**
     * 파일 시스템 상태 정보 반환
     * 
     * @param folder 확인할 폴더
     * @return 폴더 상태 정보
     */
    public Map<String, Object> getFolderStatus(Path folder) {
        try {
            boolean exists = folderExists(folder);
            List<Path> files = exists ? getSupportedFilesInFolder(folder) : List.of();
            
            return Map.of(
                CommonConstants.KEY_EXISTS, exists,
                CommonConstants.KEY_PATH, folder.toString(),
                CommonConstants.KEY_FILE_COUNT, files.size(),
                CommonConstants.KEY_FILES, files.stream().map(Path::getFileName).map(Object::toString).toList()
            );
        } catch (Exception e) {
            log.error("폴더 상태 확인 실패: {} - {}", folder, e.getMessage());
            return Map.of(
                CommonConstants.KEY_EXISTS, false,
                CommonConstants.KEY_PATH, folder.toString(),
                "error", e.getMessage()
            );
        }
    }
}
