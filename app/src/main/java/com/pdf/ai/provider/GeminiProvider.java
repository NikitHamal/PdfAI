package com.pdf.ai.provider;

import android.util.Log;

import com.pdf.ai.model.LLMModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiProvider implements LLMProvider {
    private static final String TAG = "GeminiProvider";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    
    private final OkHttpClient client;
    private final String apiKey;
    private volatile Call currentCall;
    
    public GeminiProvider(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build();
    }
    
    @Override
    public String getProviderName() {
        return "Gemini";
    }
    
    @Override
    public boolean requiresApiKey() {
        return true;
    }
    
    @Override
    public boolean supportsThinking() {
        return true;
    }
    
    @Override
    public boolean supportsSearch() {
        return false;
    }
    
    @Override
    public boolean supportsTools() {
        return true;
    }
    
    @Override
    public boolean supportsVision() {
        return true;
    }
    
    @Override
    public void fetchModels(ModelsCallback callback) {
        String url = BASE_URL + "?key=" + apiKey;
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch models", e);
                callback.onSuccess(getFallbackModels());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onSuccess(getFallbackModels());
                    return;
                }
                
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray modelsArray = json.optJSONArray("models");
                    
                    if (modelsArray == null) {
                        callback.onSuccess(getFallbackModels());
                        return;
                    }
                    
                    List<LLMModel> models = new ArrayList<>();
                    for (int i = 0; i < modelsArray.length(); i++) {
                        JSONObject m = modelsArray.getJSONObject(i);
                        String name = m.getString("name");
                        String modelId = name.replace("models/", "");
                        
                        String displayName = m.optString("displayName", modelId);
                        
                        LLMModel model = new LLMModel(modelId, displayName, "Gemini");
                        
                        JSONObject generationConfig = m.optJSONObject("generationConfig");
                        if (generationConfig != null) {
                            int maxTokens = generationConfig.optInt("maxOutputTokens", 8192);
                            model.setContextWindow(maxTokens);
                        } else {
                            model.setContextWindow(1048576);
                        }
                        
                        JSONArray supportedMethods = m.optJSONArray("supportedGenerationMethods");
                        Map<String, Boolean> caps = new HashMap<>();
                        caps.put("chat", true);
                        caps.put("stream", true);
                        caps.put("vision", modelId.contains("vision") || modelId.contains("pro"));
                        caps.put("reasoning", modelId.contains("thinking") || modelId.contains("2.5"));
                        caps.put("tools", supportedMethods != null && containsMethod(supportedMethods, "generateContent"));
                        model.setCapabilities(caps);
                        
                        JSONObject pricing = m.optJSONObject("pricing");
                        if (pricing != null) {
                            model.setInputCost(pricing.optDouble("inputCostPerToken", 0) * 100);
                            model.setOutputCost(pricing.optDouble("outputCostPerToken", 0) * 100);
                        }
                        
                        models.add(model);
                    }
                    
                    if (models.isEmpty()) {
                        callback.onSuccess(getFallbackModels());
                    } else {
                        callback.onSuccess(models);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse models", e);
                    callback.onSuccess(getFallbackModels());
                }
            }
        });
    }
    
    private boolean containsMethod(JSONArray methods, String method) {
        for (int i = 0; i < methods.length(); i++) {
            try {
                if (method.equals(methods.getString(i))) return true;
            } catch (JSONException ignored) {}
        }
        return false;
    }
    
    private List<LLMModel> getFallbackModels() {
        List<LLMModel> models = new ArrayList<>();
        String[][] fallback = {
            {"gemini-2.5-flash", "Gemini 2.5 Flash"},
            {"gemini-2.5-pro", "Gemini 2.5 Pro"},
            {"gemini-2.0-flash", "Gemini 2.0 Flash"},
            {"gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite"},
            {"gemini-1.5-flash", "Gemini 1.5 Flash"},
            {"gemini-1.5-pro", "Gemini 1.5 Pro"}
        };
        for (String[] f : fallback) {
            LLMModel model = new LLMModel(f[0], f[1], "Gemini");
            model.setContextWindow(1048576);
            Map<String, Boolean> caps = new HashMap<>();
            caps.put("chat", true);
            caps.put("stream", true);
            caps.put("vision", true);
            caps.put("reasoning", f[0].contains("2.5"));
            caps.put("tools", true);
            model.setCapabilities(caps);
            models.add(model);
        }
        return models;
    }
    
    @Override
    public void generateStream(
        List<Map<String, String>> messages,
        String model,
        Map<String, Object> options,
        StreamCallback callback
    ) {
        try {
            String modelId = model != null && !model.isEmpty() ? model : "gemini-2.5-flash";
            String url = BASE_URL + modelId + ":streamGenerateContent?key=" + apiKey + "&alt=sse";
            
            JSONArray contents = new JSONArray();
            for (Map<String, String> msg : messages) {
                JSONObject content = new JSONObject();
                String role = "user".equals(msg.get("role")) ? "user" : "model";
                content.put("role", role);
                
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", msg.get("content"));
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
            }
            
            JSONObject payload = new JSONObject();
            payload.put("contents", contents);
            
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", options != null && options.containsKey("temperature")
                ? ((Number) options.get("temperature")).doubleValue() : 0.7);
            generationConfig.put("maxOutputTokens", options != null && options.containsKey("max_tokens")
                ? options.get("max_tokens") : 8192);
            payload.put("generationConfig", generationConfig);
            
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .addHeader("Accept", "text/event-stream")
                .build();
            
            currentCall = client.newCall(request);
            
            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError("HTTP " + response.code());
                        return;
                    }
                    
                    try {
                        String line;
                        StringBuilder buffer = new StringBuilder();
                        byte[] bufferBytes = new byte[8192];
                        int bytesRead;
                        
                        while ((bytesRead = response.body().byteStream().read(bufferBytes)) != -1) {
                            buffer.append(new String(bufferBytes, 0, bytesRead, "UTF-8"));
                            
                            int newlineIdx;
                            while ((newlineIdx = buffer.indexOf("\n")) != -1) {
                                line = buffer.substring(0, newlineIdx);
                                buffer.delete(0, newlineIdx + 1);
                                
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    handleStreamData(data, callback);
                                }
                            }
                        }
                        
                        callback.onComplete();
                    } catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                }
            });
            
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }
    
    private void handleStreamData(String data, StreamCallback callback) {
        if (data == null || data.isEmpty() || "[DONE]".equals(data.trim())) return;
        
        try {
            JSONObject json = new JSONObject(data);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return;
            
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.optJSONObject("content");
            if (content == null) return;
            
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) return;
            
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.getJSONObject(i);
                String text = part.optString("text", null);
                if (text != null && !text.isEmpty()) {
                    callback.onText(text);
                }
                
                if (part.has("functionCall")) {
                    JSONObject fnCall = part.getJSONObject("functionCall");
                    String name = fnCall.optString("name", "");
                    String args = fnCall.optString("args", "{}");
                    callback.onToolCall("call_" + System.currentTimeMillis(), name, args);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Malformed stream data: " + e.getMessage());
        }
    }
    
    @Override
    public void cancel() {
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }
}