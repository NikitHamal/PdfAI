package com.pdf.ai;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_SUGGESTIONS = 5;

    private int type;
    private String message; // For user/AI text messages
    
    // New fields for Qwen features
    private String thinkingContent; // For thinking mode content
    private String webSearchContent; // For web search content
    private boolean hasThinking; // Flag to indicate if message has thinking
    private boolean hasWebSearch; // Flag to indicate if message has web search

    // Constructor for user and AI messages
    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // Constructor for suggestion messages
     public ChatMessage(int type) {
        this(type, null, null, 0);
    }


    // Getters and setters
    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
