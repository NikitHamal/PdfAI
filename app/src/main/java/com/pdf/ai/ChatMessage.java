package com.pdf.ai;

import com.pdf.ai.OutlineData;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_PROGRESS = 2;
    public static final int TYPE_OUTLINE = 3;
    public static final int TYPE_PDF_DOWNLOAD = 4;
    public static final int TYPE_SUGGESTIONS = 5;

    private int type;
    private String message; // For user/AI text messages
    private String progressStatus; // For progress messages
    private int progressValue; // For determinate progress (0-100)
    private OutlineData outlineData; // For outline messages
    private String filePath; // For PDF download messages
    private String pdfTitle; // For PDF download messages
    
    // New fields for Qwen features
    private String thinkingContent; // For thinking mode content
    private String webSearchContent; // For web search content
    private boolean hasThinking; // Flag to indicate if message has thinking
    private boolean hasWebSearch; // Flag to indicate if message has web search

    public ChatMessage(int type, String message, String progressStatus, OutlineData outlineData) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.outlineData = outlineData;
        this.progressValue = 0; // Default to 0 for non-progress messages
        this.hasThinking = false;
        this.hasWebSearch = false;
    }

    // Constructor to include progressValue
    public ChatMessage(int type, String message, String progressStatus, int progressValue, OutlineData outlineData) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = progressValue;
        this.outlineData = outlineData;
        this.hasThinking = false;
        this.hasWebSearch = false;
    }

    public ChatMessage(int type, String message, String progressStatus, OutlineData outlineData, String filePath, String pdfTitle) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.outlineData = outlineData;
        this.filePath = filePath;
        this.pdfTitle = pdfTitle;
        this.progressValue = 0; // Default to 0 for non-progress messages
        this.hasThinking = false;
        this.hasWebSearch = false;
    }

    // Constructor to include progressValue for PDF download type
    public ChatMessage(int type, String message, String progressStatus, int progressValue, OutlineData outlineData, String filePath, String pdfTitle) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = progressValue;
        this.outlineData = outlineData;
        this.filePath = filePath;
        this.pdfTitle = pdfTitle;
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

    public OutlineData getOutlineData() {
        return outlineData;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getPdfTitle() {
        return pdfTitle;
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
