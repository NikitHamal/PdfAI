package com.pdf.ai.provider;

import com.pdf.ai.model.LLMModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderFactory {
    
    private static final Map<String, Class<? extends LLMProvider>> PROVIDERS = new HashMap<>();
    
    static {
        PROVIDERS.put("DeepInfra", DeepInfraProvider.class);
        PROVIDERS.put("Qwen", QwenProvider.class);
        PROVIDERS.put("Gemini", GeminiProvider.class);
    }
    
    public static LLMProvider create(String providerName) {
        return create(providerName, null);
    }
    
    public static LLMProvider create(String providerName, String apiKey) {
        switch (providerName) {
            case "DeepInfra":
                return new DeepInfraProvider();
            case "Qwen":
                return new QwenProvider();
            case "Gemini":
                if (apiKey == null || apiKey.isEmpty()) return null;
                return new GeminiProvider(apiKey);
            default:
                return new DeepInfraProvider();
        }
    }
    
    public static List<String> getAvailableProviders() {
        List<String> providers = new ArrayList<>();
        providers.add("DeepInfra");
        providers.add("Qwen");
        providers.add("Gemini");
        return providers;
    }
    
    public static List<LLMModel> getStaticModels(String providerName) {
        List<LLMModel> models = new ArrayList<>();
        
        if ("DeepInfra".equals(providerName)) {
            String[][] deepinfraModels = {
                {"Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B"},
                {"Qwen/Qwen2.5-72B-Instruct", "Qwen 2.5 72B"},
                {"meta-llama/Meta-Llama-3.1-8B-Instruct", "Llama 3.1 (8B)"},
                {"meta-llama/Meta-Llama-3.3-70B-Instruct", "Llama 3.3 (70B)"},
                {"deepseek-ai/DeepSeek-V3.2", "DeepSeek V3.2"},
                {"zai-org/GLM-4.7-Flash", "GLM 4.7 Flash"}
            };
            for (String[] m : deepinfraModels) {
                LLMModel model = new LLMModel(m[0], m[1], "DeepInfra");
                model.setContextWindow(32768);
                Map<String, Boolean> caps = new HashMap<>();
                caps.put("chat", true);
                caps.put("stream", true);
                caps.put("vision", false);
                caps.put("reasoning", m[0].contains("DeepSeek"));
                caps.put("tools", true);
                model.setCapabilities(caps);
                models.add(model);
            }
        } else if ("Qwen".equals(providerName)) {
            String[][] qwenModels = {
                {"qwen3.6-plus", "Qwen 3.6 Plus"},
                {"qwen3-235b-a22b", "Qwen 3 235B A22B"},
                {"qwen2.5-max", "Qwen 2.5 Max"},
                {"qwen2.5-72b-instruct", "Qwen 2.5 72B"},
                {"qwen2.5-32b-instruct", "Qwen 2.5 32B"}
            };
            for (String[] m : qwenModels) {
                LLMModel model = new LLMModel(m[0], m[1], "Qwen");
                model.setContextWindow(131072);
                Map<String, Boolean> caps = new HashMap<>();
                caps.put("chat", true);
                caps.put("stream", true);
                caps.put("vision", false);
                caps.put("reasoning", true);
                caps.put("tools", true);
                model.setCapabilities(caps);
                models.add(model);
            }
        }
        
        return models;
    }
    
    public static boolean requiresApiKey(String providerName) {
        return "Gemini".equals(providerName);
    }
}