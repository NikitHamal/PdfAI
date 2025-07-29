package com.pdf.ai;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "PdfAiPrefs";
    private static final String API_KEY_KEY = "geminiApiKey";
    private static final String KEY_SELECTED_MODEL = "selectedModel";
    private static final String KEY_THINKING_ENABLED = "thinkingEnabled";
    private static final String KEY_WEB_SEARCH_ENABLED = "webSearchEnabled";

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

    public void saveSelectedModel(String model) {
        sharedPreferences.edit().putString(KEY_SELECTED_MODEL, model).apply();
    }

    public String getSelectedModel() {
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "qwen3-235b-a22b");
    }

    public void saveThinkingEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_THINKING_ENABLED, enabled).apply();
    }

    public boolean isThinkingEnabled() {
        return sharedPreferences.getBoolean(KEY_THINKING_ENABLED, false);
    }

    public void saveWebSearchEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_WEB_SEARCH_ENABLED, enabled).apply();
    }

    public boolean isWebSearchEnabled() {
        return sharedPreferences.getBoolean(KEY_WEB_SEARCH_ENABLED, false);
    }
}
