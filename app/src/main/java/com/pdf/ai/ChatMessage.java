package com.pdf.ai;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_PROGRESS = 2;
    public static final int TYPE_SUGGESTIONS = 3;

    private int type;
    private String message; // For user/AI text messages
    private String progressStatus; // For progress messages
    private int progressValue; // For determinate progress (0-100)
    
    // New fields for Qwen features
    private String thinkingContent; // For thinking mode content
    private String webSearchContent; // For web search content
    private boolean hasThinking; // Flag to indicate if message has thinking
    private boolean hasWebSearch; // Flag to indicate if message has web search

    public ChatMessage(int type, String message, String progressStatus) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = 0; // Default to 0 for non-progress messages
        this.hasThinking = false;
        this.hasWebSearch = false;
    }

    // Constructor to include progressValue
    public ChatMessage(int type, String message, String progressStatus, int progressValue) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = progressValue;
        this.hasThinking = false;
        this.hasWebSearch = false;
    }

    // Getters and setters
    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(int progressValue) {
        this.progressValue = progressValue;
    }

    // New getters and setters for thinking and web search
    public String getThinkingContent() {
        return thinkingContent;
    }

    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
        this.hasThinking = thinkingContent != null && !thinkingContent.isEmpty();
    }

    public String getWebSearchContent() {
        return webSearchContent;
    }

    public void setWebSearchContent(String webSearchContent) {
        this.webSearchContent = webSearchContent;
        this.hasWebSearch = webSearchContent != null && !webSearchContent.isEmpty();
    }

    public boolean hasThinking() {
        return hasThinking;
    }

    public boolean hasWebSearch() {
        return hasWebSearch;
    }
}
