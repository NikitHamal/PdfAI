package com.pdf.ai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QwenApiClient {

    private static final String BASE_URL = "https://chat.qwen.ai/api/v2/";
    private static final String NEW_CHAT_URL = BASE_URL + "chats/new";
    private static final String COMPLETION_URL = BASE_URL + "chat/completions";
    private static final String MODELS_URL = "https://chat.qwen.ai/api/models";
    
    // Hardcoded authorization token and headers from documentation
    private static final String AUTHORIZATION_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDk5NzB9.OEvpJhnzhUNFVMKb3d6UhtQBlQKypl3UcLRGUbm07H0";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36";

    private final OkHttpClient client;

    public QwenApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface QwenApiCallback {
        void onSuccess(String response);
        void onFailure(String error);
        void onStreamUpdate(String partialResponse);
    }

    public interface NewChatCallback {
        void onSuccess(String chatId);
        void onFailure(String error);
    }

    // Create a new chat session
    public void createNewChat(String title, String[] models, NewChatCallback callback) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            JSONArray modelsArray = new JSONArray();
            for (String model : models) {
                modelsArray.put(model);
            }
            requestBody.put("models", modelsArray);
            requestBody.put("chat_mode", "normal");
            requestBody.put("chat_type", "t2t");
            requestBody.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(NEW_CHAT_URL)
                .post(body)
                .addHeader("Authorization", AUTHORIZATION_TOKEN)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("source", "h5")
                .addHeader("bx-v", "2.5.31")
                .addHeader("x-request-id", UUID.randomUUID().toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseStr = response.body().string();
                        JSONObject responseJson = new JSONObject(responseStr);
                        if (responseJson.getBoolean("success")) {
                            JSONObject data = responseJson.getJSONObject("data");
                            String chatId = data.getString("id");
                            callback.onSuccess(chatId);
                        } else {
                            callback.onFailure("Failed to create chat");
                        }
                    } catch (JSONException e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure("Error: " + response.code() + " - " + errorBody);
                }
            }
        });
    }

    // Send completion request
    public void sendCompletion(String chatId, String model, String message, String parentId, 
                              boolean thinkingEnabled, boolean webSearchEnabled, QwenApiCallback callback) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("stream", true);
            requestBody.put("incremental_output", true);
            requestBody.put("chat_id", chatId);
            requestBody.put("chat_mode", "normal");
            requestBody.put("model", model);
            requestBody.put("parent_id", parentId);
            requestBody.put("timestamp", System.currentTimeMillis());

            // Create message object
            JSONObject messageObj = new JSONObject();
            messageObj.put("fid", UUID.randomUUID().toString());
            messageObj.put("parentId", parentId);
            messageObj.put("childrenIds", new JSONArray());
            messageObj.put("role", "user");
            messageObj.put("content", message);
            messageObj.put("user_action", "chat");
            messageObj.put("files", new JSONArray());
            messageObj.put("timestamp", System.currentTimeMillis());
            
            JSONArray modelsArray = new JSONArray();
            modelsArray.put(model);
            messageObj.put("models", modelsArray);
            
            // Set chat type based on features
            String chatType = webSearchEnabled ? "search" : "t2t";
            messageObj.put("chat_type", chatType);
            
            // Feature config
            JSONObject featureConfig = new JSONObject();
            featureConfig.put("thinking_enabled", thinkingEnabled);
            featureConfig.put("output_schema", "phase");
            
            if (thinkingEnabled) {
                featureConfig.put("thinking_budget", 38912);
            }
            if (webSearchEnabled) {
                featureConfig.put("search_version", "v2");
            }
            
            messageObj.put("feature_config", featureConfig);
            
            JSONObject extra = new JSONObject();
            JSONObject meta = new JSONObject();
            meta.put("subChatType", chatType);
            extra.put("meta", meta);
            messageObj.put("extra", extra);
            
            messageObj.put("sub_chat_type", chatType);
            messageObj.put("parent_id", parentId);

            JSONArray messages = new JSONArray();
            messages.put(messageObj);
            requestBody.put("messages", messages);

        } catch (JSONException e) {
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        String url = COMPLETION_URL + "?chat_id=" + chatId;
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", AUTHORIZATION_TOKEN)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("accept", "*/*")
                .addHeader("content-type", "application/json")
                .addHeader("source", "h5")
                .addHeader("bx-v", "2.5.31")
                .addHeader("x-accel-buffering", "no")
                .addHeader("x-request-id", UUID.randomUUID().toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Handle streaming response
                    handleStreamingResponse(response, callback);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure("Error: " + response.code() + " - " + errorBody);
                }
            }
        });
    }

    private void handleStreamingResponse(Response response, QwenApiCallback callback) throws IOException {
        String responseBody = response.body().string();
        String[] lines = responseBody.split("\n");
        
        StringBuilder thinkingContent = new StringBuilder();
        StringBuilder answerContent = new StringBuilder();
        StringBuilder webSearchContent = new StringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6);
                try {
                    JSONObject data = new JSONObject(jsonData);
                    
                    if (data.has("choices")) {
                        JSONArray choices = data.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            if (choice.has("delta")) {
                                JSONObject delta = choice.getJSONObject("delta");
                                String content = delta.optString("content", "");
                                String phase = delta.optString("phase", "");
                                String status = delta.optString("status", "");
                                
                                switch (phase) {
                                    case "think":
                                        thinkingContent.append(content);
                                        break;
                                    case "answer":
                                        answerContent.append(content);
                                        break;
                                    case "web_search":
                                        webSearchContent.append(content);
                                        break;
                                }
                                
                                // Send streaming update
                                callback.onStreamUpdate(content);
                                
                                if ("finished".equals(status)) {
                                    // Completion finished
                                    String finalResponse = buildFinalResponse(thinkingContent.toString(), 
                                                                           answerContent.toString(), 
                                                                           webSearchContent.toString());
                                    callback.onSuccess(finalResponse);
                                    return;
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.w("QwenApiClient", "Failed to parse streaming data: " + e.getMessage());
                }
            }
        }
    }

    private String buildFinalResponse(String thinking, String answer, String webSearch) {
        JSONObject response = new JSONObject();
        try {
            response.put("thinking", thinking);
            response.put("answer", answer);
            response.put("web_search", webSearch);
            response.put("has_thinking", !thinking.isEmpty());
            response.put("has_web_search", !webSearch.isEmpty());
            return response.toString();
        } catch (JSONException e) {
            return answer; // Fallback to just answer
        }
    }

    // Get available models (this would be hardcoded based on the documentation)
    public static Map<String, ModelInfo> getAvailableModels() {
        Map<String, ModelInfo> models = new HashMap<>();

        models.put("qwen3-coder-plus", new ModelInfo("Qwen3-Coder", "A strong coding agent capable of long-horizon tasks", true, true, true, true, true, true, true, true));
        models.put("qwen3-235b-a22b", new ModelInfo("Qwen3-235B-A22B-2507", "The most powerful mixture-of-experts language model", true, true, true, true, true, true, true, true));
        models.put("qwen3-30b-a3b", new ModelInfo("Qwen3-30B-A3B", "A compact and high-performance Mixture of Experts (MoE) model", true, true, true, true, true, true, true, true));
        models.put("qwen3-32b", new ModelInfo("Qwen3-32B", "The most powerful dense model", true, true, true, true, true, true, true, true));
        models.put("qwen-max-latest", new ModelInfo("Qwen2.5-Max", "The most powerful language model in the Qwen series", true, true, true, true, true, true, false, true));
        models.put("qwen-plus-2025-01-25", new ModelInfo("Qwen2.5-Plus", "Capable of complex tasks", true, true, true, true, true, true, false, true));
        models.put("qwq-32b", new ModelInfo("QwQ-32B", "Capable of thinking and reasoning", false, true, false, false, false, false, false, false));
        models.put("qwen-turbo-2025-02-11", new ModelInfo("Qwen2.5-Turbo", "Fast and 1M-token context", true, true, true, true, true, true, false, true));
        models.put("qwen2.5-omni-7b", new ModelInfo("Qwen2.5-Omni-7B", "Omni model supporting voice chat and video chat", true, true, true, true, true, false, false, true));
        models.put("qvq-72b-preview-0310", new ModelInfo("QVQ-Max", "Powerful visual reasoning model", true, true, true, true, true, false, false, true));
        models.put("qwen2.5-vl-32b-instruct", new ModelInfo("Qwen2.5-VL-32B-Instruct", "Second-largest vision-language model", true, true, true, true, true, false, false, true));
        models.put("qwen2.5-14b-instruct-1m", new ModelInfo("Qwen2.5-14B-Instruct-1M", "Long-context open model", true, true, true, true, true, false, false, true));
        models.put("qwen2.5-coder-32b-instruct", new ModelInfo("Qwen2.5-Coder-32B-Instruct", "Strong at coding tasks", true, true, true, true, true, false, false, true));
        models.put("qwen2.5-72b-instruct", new ModelInfo("Qwen2.5-72B-Instruct", "Smart large language model", true, true, true, true, true, false, false, true));

        // Gemini models (existing ones - these don't support thinking/web search)
        models.put("gemini-1.5-flash-latest", new ModelInfo("Gemini 1.5 Flash Latest", "Fast and efficient", false, false, false, false, false, false, false, false));
        models.put("gemini-1.5-pro-latest", new ModelInfo("Gemini 1.5 Pro Latest", "Most capable model", false, false, false, false, false, false, false, false));
        
        return models;
    }

    public static class ModelInfo {
        public final String displayName;
        public final String description;
        public final boolean supportsDocument;
        public final boolean supportsVision;
        public final boolean supportsVideo;
        public final boolean supportsAudio;
        public final boolean supportsCitations;
        public final boolean supportsThinking;
        public final boolean supportsWebSearch;
        public final boolean isQwenModel;


        public ModelInfo(String displayName, String description, boolean supportsDocument, boolean supportsVision, boolean supportsVideo, boolean supportsAudio, boolean supportsCitations, boolean supportsThinking, boolean supportsWebSearch, boolean isQwenModel) {
            this.displayName = displayName;
            this.description = description;
            this.supportsDocument = supportsDocument;
            this.supportsVision = supportsVision;
            this.supportsVideo = supportsVideo;
            this.supportsAudio = supportsAudio;
            this.supportsCitations = supportsCitations;
            this.supportsThinking = supportsThinking;
            this.supportsWebSearch = supportsWebSearch;
            this.isQwenModel = isQwenModel;
        }
    }
}