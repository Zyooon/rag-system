package com.example.rag_project.dto;

import lombok.Data;

@Data
public class RagRequest {
    private String query;
    private String filePath;
}
