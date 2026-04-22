package com.pdf.ai.provider;

import com.pdf.ai.model.LLMModel;

import java.util.List;
import java.util.Map;

public interface LLMProvider {
    
    interface StreamCallback {
        void onText(String text);
        void onThinking(String thinking);
        void onToolCall(String id, String name, String arguments);
        void onComplete();
        void onError(String error);
    }
    
    interface ModelsCallback {
        void onSuccess(List<LLMModel> models);
        void onError(String error);
    }

    String getProviderName();
    
    boolean requiresApiKey();
    
    void fetchModels(ModelsCallback callback);
    
    void generateStream(
        List<Map<String, String>> messages,
        String model,
        Map<String, Object> options,
        StreamCallback callback
    );
    
    void cancel();
    
    default boolean supportsThinking() { return false; }
    default boolean supportsSearch() { return false; }
    default boolean supportsTools() { return false; }
    default boolean supportsVision() { return false; }
}