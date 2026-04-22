package com.pdf.ai.model;

import java.util.Map;

public class LLMModel {
    private String id;
    private String name;
    private String provider;
    private Map<String, Boolean> capabilities;
    private int contextWindow;
    private double inputCost;
    private double outputCost;

    public LLMModel() {}

    public LLMModel(String id, String name, String provider) {
        this.id = id;
        this.name = name;
        this.provider = provider;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public Map<String, Boolean> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Boolean> capabilities) { this.capabilities = capabilities; }
    
    public int getContextWindow() { return contextWindow; }
    public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }
    
    public double getInputCost() { return inputCost; }
    public void setInputCost(double inputCost) { this.inputCost = inputCost; }
    
    public double getOutputCost() { return outputCost; }
    public void setOutputCost(double outputCost) { this.outputCost = outputCost; }
    
    public boolean supportsChat() { return capabilities != null && capabilities.getOrDefault("chat", true); }
    public boolean supportsStreaming() { return capabilities != null && capabilities.getOrDefault("stream", true); }
    public boolean supportsVision() { return capabilities != null && capabilities.getOrDefault("vision", false); }
    public boolean supportsReasoning() { return capabilities != null && capabilities.getOrDefault("reasoning", false); }
    public boolean supportsTools() { return capabilities != null && capabilities.getOrDefault("tools", true); }
    
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        if (id != null) {
            String[] parts = id.split("/");
            return parts[parts.length - 1];
        }
        return "Unknown";
    }
    
    public String getFullId() {
        return provider + "/" + id;
    }
}