package com.pdf.ai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Conversation {
    private String id;
    private String title;
    private String provider;
    private String modelId;
    private long createdAt;
    private long updatedAt;
    private List<ConversationMessage> messages;
    private boolean thinkingEnabled;
    private boolean searchEnabled;
    private String customPrompt;

    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.messages = new ArrayList<>();
        this.thinkingEnabled = false;
        this.searchEnabled = false;
    }

    public Conversation(String title, String provider, String modelId) {
        this();
        this.title = title;
        this.provider = provider;
        this.modelId = modelId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public List<ConversationMessage> getMessages() { return messages; }
    public void setMessages(List<ConversationMessage> messages) { this.messages = messages; }
    
    public boolean isThinkingEnabled() { return thinkingEnabled; }
    public void setThinkingEnabled(boolean thinkingEnabled) { this.thinkingEnabled = thinkingEnabled; }
    
    public boolean isSearchEnabled() { return searchEnabled; }
    public void setSearchEnabled(boolean searchEnabled) { this.searchEnabled = searchEnabled; }
    
    public String getCustomPrompt() { return customPrompt; }
    public void setCustomPrompt(String customPrompt) { this.customPrompt = customPrompt; }
    
    public void addMessage(ConversationMessage message) {
        this.messages.add(message);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public String getPreview() {
        if (messages == null || messages.isEmpty()) return "New conversation";
        ConversationMessage last = messages.get(messages.size() - 1);
        String content = last.getContent();
        if (content == null || content.isEmpty()) return "Empty message";
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }
}