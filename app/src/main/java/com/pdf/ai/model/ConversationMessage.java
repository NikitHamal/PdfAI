package com.pdf.ai.model;

import java.util.ArrayList;
import java.util.List;

public class ConversationMessage {
    private String role;
    private String content;
    private List<ToolCall> toolCalls;
    private String thinking;
    private List<String> searchResults;
    private long timestamp;

    public ConversationMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ConversationMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    
    public String getThinking() { return thinking; }
    public void setThinking(String thinking) { this.thinking = thinking; }
    
    public List<String> getSearchResults() { return searchResults; }
    public void setSearchResults(List<String> searchResults) { this.searchResults = searchResults; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public void addToolCall(ToolCall toolCall) {
        if (this.toolCalls == null) this.toolCalls = new ArrayList<>();
        this.toolCalls.add(toolCall);
    }
    
    public boolean isUser() { return "user".equals(role); }
    public boolean isAssistant() { return "assistant".equals(role); }
    public boolean isSystem() { return "system".equals(role); }
}