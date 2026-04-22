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

public class QwenProvider implements LLMProvider {
    private static final String TAG = "QwenProvider";
    private static final String BASE_URL = "https://chat.qwen.ai";
    private static final String API_URL = BASE_URL + "/api/v2/chat/completions";
    private static final String MODELS_URL = BASE_URL + "/api/v2/models";
    private static final String AUTH_URL = BASE_URL + "/api/v1/auths/";
    
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    };
    
    private final OkHttpClient client;
    private final Random random;
    private volatile EventSource currentEventSource;
    private volatile Call currentCall;
    private volatile String cachedMidtoken;
    private volatile long midtokenTime;
    private static final long MIDTOKEN_TTL = 5 * 60 * 1000;
    
    public QwenProvider() {
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
        return "Qwen";
    }
    
    @Override
    public boolean requiresApiKey() {
        return false;
    }
    
    @Override
    public boolean supportsThinking() {
        return true;
    }
    
    @Override
    public boolean supportsSearch() {
        return true;
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
        callback.onSuccess(getFallbackModels());
    }
    
    private List<LLMModel> getFallbackModels() {
        List<LLMModel> models = new ArrayList<>();
        String[][] fallback = {
            {"qwen3.6-plus", "Qwen 3.6 Plus", "131072"},
            {"qwen3-235b-a22b", "Qwen 3 235B A22B", "131072"},
            {"qwen2.5-max", "Qwen 2.5 Max", "131072"},
            {"qwen2.5-72b-instruct", "Qwen 2.5 72B", "131072"},
            {"qwen2.5-32b-instruct", "Qwen 2.5 32B", "131072"},
            {"qwen2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B", "131072"}
        };
        for (String[] f : fallback) {
            LLMModel model = new LLMModel(f[0], f[1], "Qwen");
            model.setContextWindow(Integer.parseInt(f[2]));
            Map<String, Boolean> caps = new HashMap<>();
            caps.put("chat", true);
            caps.put("stream", true);
            caps.put("vision", f[0].contains("vl") || f[0].contains("vision"));
            caps.put("reasoning", true);
            caps.put("tools", true);
            model.setCapabilities(caps);
            model.setInputCost(0);
            model.setOutputCost(0);
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
        new Thread(() -> {
            try {
                String modelId = model != null && !model.isEmpty() ? model : "qwen3.6-plus";
                boolean thinkingEnabled = options != null && Boolean.TRUE.equals(options.get("thinkingEnabled"));
                boolean searchEnabled = options != null && Boolean.TRUE.equals(options.get("searchEnabled"));
                String thinkingMode = options != null ? (String) options.get("thinkingMode") : null;
                if (thinkingMode == null) thinkingMode = "Auto";
                
                String midtoken = getMidtoken();
                String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
                
                JSONArray messagesArray = new JSONArray();
                for (Map<String, String> msg : messages) {
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", msg.get("role"));
                    msgObj.put("content", msg.get("content"));
                    messagesArray.put(msgObj);
                }
                
                JSONObject payload = new JSONObject();
                payload.put("model", modelId);
                payload.put("messages", messagesArray);
                payload.put("stream", true);
                payload.put("temperature", options != null && options.containsKey("temperature")
                    ? ((Number) options.get("temperature")).doubleValue() : 0.7);
                
                if (thinkingEnabled) {
                    JSONObject featureConfig = new JSONObject();
                    featureConfig.put("thinking_enabled", true);
                    featureConfig.put("auto_thinking", "Auto".equals(thinkingMode));
                    featureConfig.put("thinking_mode", thinkingMode);
                    featureConfig.put("research_mode", searchEnabled ? "deep" : "normal");
                    featureConfig.put("auto_search", searchEnabled);
                    payload.put("feature_config", featureConfig);
                }
                
                Request.Builder builder = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(payload.toString(), MediaType.get("application/json")));
                
                builder.addHeader("Accept", "text/event-stream");
                builder.addHeader("Content-Type", "application/json");
                builder.addHeader("Origin", BASE_URL);
                builder.addHeader("Referer", BASE_URL + "/");
                builder.addHeader("User-Agent", userAgent);
                
                if (midtoken != null) {
                    builder.addHeader("bx-umidtoken", midtoken);
                    builder.addHeader("bx-v", "2.5.31");
                }
                
                Request request = builder.build();
                
                StringBuilder fullContent = new StringBuilder();
                StringBuilder thinkingContent = new StringBuilder();
                
                EventSourceListener listener = new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        handleSseEvent(data, callback, fullContent, thinkingContent);
                    }
                    
                    @Override
                    public void onClosed(EventSource eventSource) {
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
                
            } catch (Exception e) {
                Log.e(TAG, "Error in generateStream", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    private String getMidtoken() {
        if (cachedMidtoken != null && System.currentTimeMillis() - midtokenTime < MIDTOKEN_TTL) {
            return cachedMidtoken;
        }
        
        try {
            Request request = new Request.Builder()
                .url("https://chat.qwen.ai/api/v1/auths/")
                .get()
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                .build();
            
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                if (json.has("data")) {
                    JSONObject data = json.getJSONObject("data");
                    if (data.has("midtoken")) {
                        cachedMidtoken = data.getString("midtoken");
                        midtokenTime = System.currentTimeMillis();
                        return cachedMidtoken;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get midtoken: " + e.getMessage());
        }
        return null;
    }
    
    private void handleSseEvent(String data, StreamCallback callback,
                               StringBuilder fullContent, StringBuilder thinkingContent) {
        if (data == null || data.isEmpty()) return;
        
        try {
            JSONObject json = new JSONObject(data);
            String eventType = json.optString("type", "");
            
            if ("thread.item_updated".equals(eventType) || "thread.message_delta".equals(eventType)) {
                JSONObject update = json.optJSONObject("update");
                if (update == null) update = json.optJSONObject("entry");
                if (update == null) return;
                
                String entryType = update.optString("type", "");
                
                if ("assistant_message.content_part.text_delta".equals(entryType)) {
                    String delta = update.optString("delta", "");
                    if (!delta.isEmpty()) {
                        fullContent.append(delta);
                        callback.onText(delta);
                    }
                } else if ("assistant_message.content_part.thinking_delta".equals(entryType)) {
                    String delta = update.optString("delta", "");
                    if (!delta.isEmpty()) {
                        thinkingContent.append(delta);
                        callback.onThinking(delta);
                    }
                } else if ("assistant_message.tool_call".equals(entryType)) {
                    String tcId = update.optString("id", "call_" + System.currentTimeMillis());
                    JSONObject fn = update.optJSONObject("function");
                    if (fn != null) {
                        String name = fn.optString("name", "");
                        String args = fn.optString("arguments", "{}");
                        callback.onToolCall(tcId, name, args);
                    }
                }
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