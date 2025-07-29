package com.pdf.ai;

import android.content.Context;
import android.content.SharedPreferences;
import com.pdf.ai.ChatMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {

    private static final String PREFS_NAME = "PdfAiPrefs";
    private static final String API_KEY_KEY = "geminiApiKey";
    private static final String CHAT_HISTORY_KEY = "chatHistory";
    private static final String KEY_SELECTED_MODEL = "selectedModel";

    private SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getGeminiApiKey() {
        return sharedPreferences.getString(API_KEY_KEY, null);
    }

    public void setGeminiApiKey(String apiKey) {
        sharedPreferences.edit().putString(API_KEY_KEY, apiKey).apply();
    }

    public void setSelectedModel(String model) {
        sharedPreferences.edit().putString(KEY_SELECTED_MODEL, model).apply();
    }

    public String getSelectedModel() {
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "gemini-1.5-flash-latest");
    }

    public void saveChatHistory(List<ChatMessage> chatMessages) {
        JSONArray jsonArray = new JSONArray();
        for (ChatMessage message : chatMessages) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", message.getType());
                if (message.getMessage() != null) {
                    jsonObject.put("message", message.getMessage());
                }
                if (message.getProgressStatus() != null) {
                    jsonObject.put("progressStatus", message.getProgressStatus());
                }
                jsonObject.put("progressValue", message.getProgressValue());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPreferences.edit().putString(CHAT_HISTORY_KEY, jsonArray.toString()).apply();
    }

    public List<ChatMessage> loadChatHistory() {
        List<ChatMessage> chatMessages = new ArrayList<>();
        String historyJson = sharedPreferences.getString(CHAT_HISTORY_KEY, null);
        if (historyJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(historyJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int type = jsonObject.getInt("type");
                    String message = jsonObject.optString("message", null);
                    String progressStatus = jsonObject.optString("progressStatus", null);
                    int progressValue = jsonObject.optInt("progressValue", 0);

                    chatMessages.add(new ChatMessage(type, message, progressStatus, progressValue));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return chatMessages;
    }
}
