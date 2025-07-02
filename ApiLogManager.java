package com.pdf.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ApiLogManager {

    private static final String PREFS_NAME = "ApiLogPrefs";
    private static final String API_LOG_KEY = "apiLog";
    private SharedPreferences sharedPreferences;

    public ApiLogManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addLog(String prompt, String response) {
        List<ApiLog> logs = getLogs();
        logs.add(new ApiLog(prompt, response));
        saveLogs(logs);
    }

    public List<ApiLog> getLogs() {
        List<ApiLog> logs = new ArrayList<>();
        String logJson = sharedPreferences.getString(API_LOG_KEY, null);
        if (logJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(logJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    logs.add(new ApiLog(jsonObject.getString("prompt"), jsonObject.getString("response")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return logs;
    }

    private void saveLogs(List<ApiLog> logs) {
        JSONArray jsonArray = new JSONArray();
        for (ApiLog log : logs) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("prompt", log.getPrompt());
                jsonObject.put("response", log.getResponse());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPreferences.edit().putString(API_LOG_KEY, jsonArray.toString()).apply();
    }

    public static class ApiLog {
        private String prompt;
        private String response;

        public ApiLog(String prompt, String response) {
            this.prompt = prompt;
            this.response = response;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getResponse() {
            return response;
        }
    }
}