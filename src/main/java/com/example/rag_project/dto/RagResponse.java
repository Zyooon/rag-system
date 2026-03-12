package com.example.rag_project.dto;

import lombok.Data;

@Data
public class RagResponse {
    private String answer;
    private boolean success;
    private String message;
    
    public static RagResponse success(String answer) {
        RagResponse response = new RagResponse();
        response.setSuccess(true);
        response.setAnswer(answer);
        return response;
    }
    
    public static RagResponse error(String message) {
        RagResponse response = new RagResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
