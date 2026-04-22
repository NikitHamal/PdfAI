package com.pdf.ai;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "PdfAiPrefs";
    private static final String API_KEY_KEY = "geminiApiKey";
    private static final String KEY_SELECTED_MODEL = "selectedModel";
    private static final String KEY_SELECTED_PROVIDER = "selectedProvider";
    private static final String KEY_THINKING_ENABLED = "thinkingEnabled";
    private static final String KEY_SEARCH_ENABLED = "searchEnabled";

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
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "Qwen/Qwen2.5-Coder-32B-Instruct");
    }

    public void setSelectedProvider(String provider) {
        sharedPreferences.edit().putString(KEY_SELECTED_PROVIDER, provider).apply();
    }

    public String getSelectedProvider() {
        return sharedPreferences.getString(KEY_SELECTED_PROVIDER, "DeepInfra");
    }

    public void setThinkingEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_THINKING_ENABLED, enabled).apply();
    }

    public boolean isThinkingEnabled() {
        return sharedPreferences.getBoolean(KEY_THINKING_ENABLED, false);
    }

    public void setSearchEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SEARCH_ENABLED, enabled).apply();
    }

    public boolean isSearchEnabled() {
        return sharedPreferences.getBoolean(KEY_SEARCH_ENABLED, false);
    }
}