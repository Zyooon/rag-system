package com.example.rag_project.exception;

public class RagServiceException extends RuntimeException {
    
    private final String className;
    private final int lineNumber;
    private final String methodName;

    public RagServiceException(String message) {
        super(message);
        this.className = "Unknown";
        this.lineNumber = -1;
        this.methodName = "Unknown";
    }

    public RagServiceException(String message, Throwable cause) {
        super(message, cause);
        this.className = "Unknown";
        this.lineNumber = -1;
        this.methodName = "Unknown";
    }

    public RagServiceException(String message, String className, int lineNumber, String methodName) {
        super(message);
        this.className = className;
        this.lineNumber = lineNumber;
        this.methodName = methodName;
    }

    public RagServiceException(String message, Throwable cause, String className, int lineNumber, String methodName) {
        super(message, cause);
        this.className = className;
        this.lineNumber = lineNumber;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        if (lineNumber > 0) {
            return String.format("%s: %s (at %s.%s:%d)", 
                getClass().getSimpleName(), getMessage(), className, methodName, lineNumber);
        } else {
            return String.format("%s: %s", getClass().getSimpleName(), getMessage());
        }
    }
}
