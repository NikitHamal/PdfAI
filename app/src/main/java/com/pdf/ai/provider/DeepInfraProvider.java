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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class DeepInfraProvider implements LLMProvider {
    private static final String TAG = "DeepInfraProvider";
    private static final String API_URL = "https://api.deepinfra.com/v1/openai/chat/completions";
    private static final String MODELS_URL = "https://api.deepinfra.com/models/featured";
    
    private static final String[][] BROWSER_HEADERS = {
        {
            "accept", "*/*",
            "accept-language", "en-US,en;q=0.9",
            "content-type", "application/json",
            "origin", "https://deepinfra.com",
            "referer", "https://deepinfra.com/",
            "sec-ch-ua", "\"Google Chrome\";v=\"136\", \"Chromium\";v=\"136\", \"Not.A/Brand\";v=\"99\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"Windows\"",
            "sec-fetch-dest", "empty",
            "sec-fetch-mode", "cors",
            "sec-fetch-site", "same-site",
            "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        },
        {
            "accept", "*/*",
            "accept-language", "en-US,en;q=0.9",
            "content-type", "application/json",
            "origin", "https://deepinfra.com",
            "referer", "https://deepinfra.com/",
            "sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"Windows\"",
            "sec-fetch-dest", "empty",
            "sec-fetch-mode", "cors",
            "sec-fetch-site", "same-site",
            "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0"
        }
    };
    
    private final OkHttpClient client;
    private final Random random;
    private volatile Call currentCall;
    private volatile EventSource currentEventSource;
    
    public DeepInfraProvider() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build();
        this.random = new Random();
    }
    
    @Override
    public String getProviderName() {
        return "DeepInfra";
    }
    
    @Override
    public boolean requiresApiKey() {
        return false;
    }
    
    @Override
    public boolean supportsThinking() {
        return false;
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
        return false;
    }
    
    @Override
    public void fetchModels(ModelsCallback callback) {
        Request.Builder builder = new Request.Builder()
            .url(MODELS_URL)
            .get();
        
        String[] header = BROWSER_HEADERS[random.nextInt(BROWSER_HEADERS.length)];
        for (int i = 0; i < header.length; i += 2) {
            builder.addHeader(header[i], header[i + 1]);
        }
        
        client.newCall(builder.build()).enqueue(new Callback() {
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
                    JSONArray modelsArray = new JSONArray(body);
                    List<LLMModel> models = new ArrayList<>();
                    
                    for (int i = 0; i < modelsArray.length(); i++) {
                        JSONObject m = modelsArray.getJSONObject(i);
                        if (!"text-generation".equals(m.optString("type"))) continue;
                        
                        String modelId = m.getString("model_name");
                        String displayName = modelId.contains("/") 
                            ? modelId.substring(modelId.lastIndexOf("/") + 1) 
                            : modelId;
                        
                        LLMModel model = new LLMModel(modelId, displayName, "DeepInfra");
                        
                        JSONObject pricing = m.optJSONObject("pricing");
                        if (pricing != null) {
                            model.setInputCost(pricing.optDouble("cents_per_input_token", 0));
                            model.setOutputCost(pricing.optDouble("cents_per_output_token", 0));
                        }
                        
                        int maxCtx = m.optInt("max_tokens", m.optInt("context_window", 32768));
                        model.setContextWindow(maxCtx);
                        
                        Map<String, Boolean> caps = new HashMap<>();
                        caps.put("chat", true);
                        caps.put("stream", true);
                        caps.put("vision", modelId.toLowerCase().contains("vl") || modelId.toLowerCase().contains("vision"));
                        caps.put("reasoning", modelId.toLowerCase().contains("reason") || modelId.toLowerCase().contains("think") || modelId.toLowerCase().contains("r1") || modelId.toLowerCase().contains("qwq"));
                        caps.put("tools", true);
                        model.setCapabilities(caps);
                        
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
    
    private List<LLMModel> getFallbackModels() {
        List<LLMModel> models = new ArrayList<>();
        String[][] fallback = {
            {"Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B"},
            {"Qwen/Qwen2.5-72B-Instruct", "Qwen 2.5 72B"},
            {"meta-llama/Meta-Llama-3.1-8B-Instruct", "Llama 3.1 (8B)"},
            {"meta-llama/Meta-Llama-3.3-70B-Instruct", "Llama 3.3 (70B)"},
            {"deepseek-ai/DeepSeek-V3.2", "DeepSeek V3.2"},
            {"zai-org/GLM-4.7-Flash", "GLM 4.7 Flash"}
        };
        for (String[] f : fallback) {
            LLMModel model = new LLMModel(f[0], f[1], "DeepInfra");
            model.setContextWindow(32768);
            Map<String, Boolean> caps = new HashMap<>();
            caps.put("chat", true);
            caps.put("stream", true);
            caps.put("vision", false);
            caps.put("reasoning", f[0].contains("DeepSeek"));
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
            String modelId = model != null && !model.isEmpty() ? model : "Qwen/Qwen2.5-Coder-32B-Instruct";
            
            JSONObject payload = new JSONObject();
            payload.put("model", modelId);
            payload.put("stream", true);
            payload.put("temperature", options != null && options.containsKey("temperature") 
                ? ((Number) options.get("temperature")).doubleValue() : 0.7);
            payload.put("max_tokens", options != null && options.containsKey("max_tokens")
                ? options.get("max_tokens") : 4096);
            
            JSONArray messagesArray = new JSONArray();
            for (Map<String, String> msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
                messagesArray.put(msgObj);
            }
            payload.put("messages", messagesArray);
            
            Request.Builder builder = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")));
            
            String[] header = BROWSER_HEADERS[random.nextInt(BROWSER_HEADERS.length)];
            for (int i = 0; i < header.length; i += 2) {
                builder.addHeader(header[i], header[i + 1]);
            }
            
            String fakeIp = random.nextInt(255) + "." + random.nextInt(256) + "." + 
                random.nextInt(256) + "." + (random.nextInt(254) + 1);
            builder.addHeader("x-forwarded-for", fakeIp);
            builder.addHeader("x-real-ip", fakeIp);
            builder.addHeader("client-ip", fakeIp);
            
            Request request = builder.build();
            
            StringBuilder fullContent = new StringBuilder();
            Map<Integer, JSONObject> toolCallsAccumulator = new HashMap<>();
            
            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    handleEvent(data, callback, toolCallsAccumulator, fullContent);
                }
                
                @Override
                public void onClosed(EventSource eventSource) {
                    for (JSONObject tc : toolCallsAccumulator.values()) {
                        try {
                            String tcId = tc.optString("id", "call_" + System.currentTimeMillis());
                            JSONObject fn = tc.getJSONObject("function");
                            callback.onToolCall(tcId, fn.getString("name"), fn.getString("arguments"));
                        } catch (JSONException ignored) {}
                    }
                    callback.onComplete();
                }
                
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    String error = t != null ? t.getMessage() : 
                        (response != null ? "HTTP " + response.code() : "Unknown error");
                    callback.onError(error);
                }
            };
            
            currentEventSource = EventSources.createFactory(client).newEventSource(request, listener);
            
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }
    
    private void handleEvent(String data, StreamCallback callback, 
                            Map<Integer, JSONObject> toolCallsAccumulator,
                            StringBuilder fullContent) {
        if (data == null || data.isEmpty() || "[DONE]".equals(data.trim())) return;
        
        try {
            JSONObject json = new JSONObject(data);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return;
            
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.optJSONObject("delta");
            if (delta == null) return;
            
            String content = delta.optString("content", null);
            if (content != null && !content.isEmpty()) {
                fullContent.append(content);
                callback.onText(content);
            }
            
            JSONArray toolCalls = delta.optJSONArray("tool_calls");
            if (toolCalls != null) {
                for (int i = 0; i < toolCalls.length(); i++) {
                    JSONObject tc = toolCalls.getJSONObject(i);
                    int idx = tc.optInt("index", 0);
                    
                    if (!toolCallsAccumulator.containsKey(idx)) {
                        toolCallsAccumulator.put(idx, new JSONObject()
                            .put("id", tc.optString("id", ""))
                            .put("function", new JSONObject()
                                .put("name", "")
                                .put("arguments", "")));
                    }
                    
                    JSONObject acc = toolCallsAccumulator.get(idx);
                    if (tc.has("id")) acc.put("id", tc.getString("id"));
                    JSONObject fn = tc.optJSONObject("function");
                    if (fn != null) {
                        JSONObject accFn = acc.getJSONObject("function");
                        if (fn.has("name")) accFn.put("name", accFn.getString("name") + fn.getString("name"));
                        if (fn.has("arguments")) accFn.put("arguments", accFn.getString("arguments") + fn.getString("arguments"));
                    }
                }
            }
            
            String finishReason = choice.optString("finish_reason", null);
            if (finishReason != null && !finishReason.isEmpty()) {
                for (JSONObject tc : toolCallsAccumulator.values()) {
                    try {
                        String tcId = tc.optString("id", "call_" + System.currentTimeMillis());
                        JSONObject fn = tc.getJSONObject("function");
                        callback.onToolCall(tcId, fn.getString("name"), fn.getString("arguments"));
                    } catch (JSONException ignored) {}
                }
                toolCallsAccumulator.clear();
            }
        } catch (JSONException e) {
            Log.w(TAG, "Malformed SSE event: " + e.getMessage());
        }
    }
    
    @Override
    public void cancel() {
        if (currentEventSource != null) {
            currentEventSource.cancel();
            currentEventSource = null;
        }
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }
}