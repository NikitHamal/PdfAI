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
    private int progressValue; // New: For determinate progress (0-100)
    private OutlineData outlineData; // For outline messages
    private String filePath; // For PDF download messages
    private String pdfTitle; // For PDF download messages

    public ChatMessage(int type, String message, String progressStatus, OutlineData outlineData) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.outlineData = outlineData;
        this.progressValue = 0; // Default to 0 for non-progress messages
    }

    // New constructor to include progressValue
    public ChatMessage(int type, String message, String progressStatus, int progressValue, OutlineData outlineData) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = progressValue;
        this.outlineData = outlineData;
    }

    public ChatMessage(int type, String message, String progressStatus, OutlineData outlineData, String filePath, String pdfTitle) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.outlineData = outlineData;
        this.filePath = filePath;
        this.pdfTitle = pdfTitle;
        this.progressValue = 0; // Default to 0 for non-progress messages
    }

    // New constructor to include progressValue for PDF download type
    public ChatMessage(int type, String message, String progressStatus, int progressValue, OutlineData outlineData, String filePath, String pdfTitle) {
        this.type = type;
        this.message = message;
        this.progressStatus = progressStatus;
        this.progressValue = progressValue;
        this.outlineData = outlineData;
        this.filePath = filePath;
        this.pdfTitle = pdfTitle;
    }

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

    // New getter and setter for progressValue
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
}
