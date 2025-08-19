package com.pdf.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.pdf.ai.ChatMessage;
import com.pdf.ai.OutlineData;

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
    private static final String KEY_SELECTED_PROVIDER = "selectedProvider"; // e.g., "Gemini" or "GPT-OSS"

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
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "gemini-2.5-flash");
    }

    public void setSelectedProvider(String provider) {
        sharedPreferences.edit().putString(KEY_SELECTED_PROVIDER, provider).apply();
    }

    public String getSelectedProvider() {
        return sharedPreferences.getString(KEY_SELECTED_PROVIDER, "Gemini");
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
                // Save progressValue
                jsonObject.put("progressValue", message.getProgressValue());

                if (message.getOutlineData() != null) {
                    JSONObject outlineJson = new JSONObject();
                    outlineJson.put("title", message.getOutlineData().getPdfTitle());
                    JSONArray sectionsArray = new JSONArray();
                    for (String section : message.getOutlineData().getSections()) {
                        sectionsArray.put(section);
                    }
                    outlineJson.put("sections", sectionsArray);
                    jsonObject.put("outlineData", outlineJson);
                }
                if (message.getFilePath() != null) {
                    jsonObject.put("filePath", message.getFilePath());
                }
                if (message.getPdfTitle() != null) {
                    jsonObject.put("pdfTitle", message.getPdfTitle());
                }
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
                    // Load progressValue, default to 0 if not found (for older saved messages)
                    int progressValue = jsonObject.optInt("progressValue", 0);
                    String filePath = jsonObject.optString("filePath", null);
                    String pdfTitle = jsonObject.optString("pdfTitle", null);

                    OutlineData outlineData = null;
                    if (jsonObject.has("outlineData")) {
                        JSONObject outlineJson = jsonObject.getJSONObject("outlineData");
                        String title = outlineJson.getString("title");
                        JSONArray sectionsArray = outlineJson.getJSONArray("sections");
                        List<String> sections = new ArrayList<>();
                        for (int j = 0; j < sectionsArray.length(); j++) {
                            sections.add(sectionsArray.getString(j));
                        }
                        outlineData = new OutlineData(title, sections);
                    }
                    // Use the new constructor that includes progressValue
                    chatMessages.add(new ChatMessage(type, message, progressStatus, progressValue, outlineData, filePath, pdfTitle));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return chatMessages;
    }
}
