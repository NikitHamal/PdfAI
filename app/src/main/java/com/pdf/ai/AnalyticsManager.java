package com.pdf.ai;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnalyticsManager {
    
    private static final String TAG = "AnalyticsManager";
    private static final String ANALYTICS_KEY = "app_analytics";
    private static final String USAGE_STATS_KEY = "usage_stats";
    private static final String FEATURE_USAGE_KEY = "feature_usage";
    private static final String ERROR_LOGS_KEY = "error_logs";
    
    private final PreferencesManager preferencesManager;
    private final SimpleDateFormat dateFormat;
    
    public AnalyticsManager(Context context) {
        this.preferencesManager = new PreferencesManager(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    // Track PDF generation events
    public void trackPdfGenerated(String templateName, int sectionsCount, long generationTime) {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "pdf_generated");
            event.put("template_name", templateName);
            event.put("sections_count", sectionsCount);
            event.put("generation_time_ms", generationTime);
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveEvent(event);
            updateUsageStats("pdfs_generated", 1);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking PDF generation", e);
        }
    }
    
    // Track API usage
    public void trackApiCall(String model, String operation, boolean success, long responseTime) {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "api_call");
            event.put("model", model);
            event.put("operation", operation);
            event.put("success", success);
            event.put("response_time_ms", responseTime);
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveEvent(event);
            updateUsageStats("api_calls", 1);
            if (success) {
                updateUsageStats("successful_api_calls", 1);
            } else {
                updateUsageStats("failed_api_calls", 1);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking API call", e);
        }
    }
    
    // Track feature usage
    public void trackFeatureUsage(String featureName) {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "feature_used");
            event.put("feature_name", featureName);
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveEvent(event);
            updateFeatureUsage(featureName);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking feature usage", e);
        }
    }
    
    // Track errors
    public void trackError(String errorType, String errorMessage, String context) {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "error");
            event.put("error_type", errorType);
            event.put("error_message", errorMessage);
            event.put("context", context);
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveErrorLog(event);
            updateUsageStats("errors", 1);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking error", e);
        }
    }
    
    // Track app session
    public void trackSessionStart() {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "session_start");
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveEvent(event);
            updateUsageStats("sessions", 1);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking session start", e);
        }
    }
    
    public void trackSessionEnd(long sessionDuration) {
        try {
            JSONObject event = new JSONObject();
            event.put("event_type", "session_end");
            event.put("session_duration_ms", sessionDuration);
            event.put("timestamp", dateFormat.format(new Date()));
            
            saveEvent(event);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error tracking session end", e);
        }
    }
    
    // Get analytics summary
    public JSONObject getAnalyticsSummary() {
        try {
            JSONObject summary = new JSONObject();
            
            // Usage statistics
            Map<String, Integer> usageStats = getUsageStats();
            JSONObject statsJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : usageStats.entrySet()) {
                statsJson.put(entry.getKey(), entry.getValue());
            }
            summary.put("usage_stats", statsJson);
            
            // Feature usage
            Map<String, Integer> featureUsage = getFeatureUsage();
            JSONObject featuresJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : featureUsage.entrySet()) {
                featuresJson.put(entry.getKey(), entry.getValue());
            }
            summary.put("feature_usage", featuresJson);
            
            // Recent events (last 50)
            JSONArray recentEvents = getRecentEvents(50);
            summary.put("recent_events", recentEvents);
            
            // Error summary
            JSONArray recentErrors = getRecentErrors(20);
            summary.put("recent_errors", recentErrors);
            
            return summary;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error generating analytics summary", e);
            return new JSONObject();
        }
    }
    
    // Get usage statistics
    public Map<String, Integer> getUsageStats() {
        Map<String, Integer> stats = new HashMap<>();
        try {
            String statsJson = preferencesManager.getSharedPreferences().getString(USAGE_STATS_KEY, "{}");
            JSONObject jsonObject = new JSONObject(statsJson);
            
            JSONArray names = jsonObject.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    int value = jsonObject.getInt(key);
                    stats.put(key, value);
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error loading usage stats", e);
        }
        
        return stats;
    }
    
    // Get feature usage
    public Map<String, Integer> getFeatureUsage() {
        Map<String, Integer> features = new HashMap<>();
        try {
            String featuresJson = preferencesManager.getSharedPreferences().getString(FEATURE_USAGE_KEY, "{}");
            JSONObject jsonObject = new JSONObject(featuresJson);
            
            JSONArray names = jsonObject.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    int value = jsonObject.getInt(key);
                    features.put(key, value);
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error loading feature usage", e);
        }
        
        return features;
    }
    
    // Clear analytics data
    public void clearAnalyticsData() {
        preferencesManager.getSharedPreferences().edit()
            .remove(ANALYTICS_KEY)
            .remove(USAGE_STATS_KEY)
            .remove(FEATURE_USAGE_KEY)
            .remove(ERROR_LOGS_KEY)
            .apply();
        
        Log.i(TAG, "Analytics data cleared");
    }
    
    private void saveEvent(JSONObject event) {
        try {
            JSONArray events = getEventsArray();
            events.put(event);
            
            // Keep only last 1000 events
            if (events.length() > 1000) {
                JSONArray newEvents = new JSONArray();
                for (int i = events.length() - 1000; i < events.length(); i++) {
                    newEvents.put(events.get(i));
                }
                events = newEvents;
            }
            
            preferencesManager.getSharedPreferences().edit()
                .putString(ANALYTICS_KEY, events.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Error saving event", e);
        }
    }
    
    private void saveErrorLog(JSONObject error) {
        try {
            JSONArray errors = getErrorsArray();
            errors.put(error);
            
            // Keep only last 100 errors
            if (errors.length() > 100) {
                JSONArray newErrors = new JSONArray();
                for (int i = errors.length() - 100; i < errors.length(); i++) {
                    newErrors.put(errors.get(i));
                }
                errors = newErrors;
            }
            
            preferencesManager.getSharedPreferences().edit()
                .putString(ERROR_LOGS_KEY, errors.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Error saving error log", e);
        }
    }
    
    private void updateUsageStats(String key, int increment) {
        try {
            Map<String, Integer> stats = getUsageStats();
            int currentValue = stats.getOrDefault(key, 0);
            stats.put(key, currentValue + increment);
            
            JSONObject statsJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                statsJson.put(entry.getKey(), entry.getValue());
            }
            
            preferencesManager.getSharedPreferences().edit()
                .putString(USAGE_STATS_KEY, statsJson.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Error updating usage stats", e);
        }
    }
    
    private void updateFeatureUsage(String featureName) {
        try {
            Map<String, Integer> features = getFeatureUsage();
            int currentValue = features.getOrDefault(featureName, 0);
            features.put(featureName, currentValue + 1);
            
            JSONObject featuresJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : features.entrySet()) {
                featuresJson.put(entry.getKey(), entry.getValue());
            }
            
            preferencesManager.getSharedPreferences().edit()
                .putString(FEATURE_USAGE_KEY, featuresJson.toString())
                .apply();
                
        } catch (JSONException e) {
            Log.e(TAG, "Error updating feature usage", e);
        }
    }
    
    private JSONArray getEventsArray() {
        try {
            String eventsJson = preferencesManager.getSharedPreferences().getString(ANALYTICS_KEY, "[]");
            return new JSONArray(eventsJson);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
    
    private JSONArray getErrorsArray() {
        try {
            String errorsJson = preferencesManager.getSharedPreferences().getString(ERROR_LOGS_KEY, "[]");
            return new JSONArray(errorsJson);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
    
    private JSONArray getRecentEvents(int count) {
        try {
            JSONArray allEvents = getEventsArray();
            JSONArray recentEvents = new JSONArray();
            
            int startIndex = Math.max(0, allEvents.length() - count);
            for (int i = startIndex; i < allEvents.length(); i++) {
                recentEvents.put(allEvents.get(i));
            }
            
            return recentEvents;
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
    
    private JSONArray getRecentErrors(int count) {
        try {
            JSONArray allErrors = getErrorsArray();
            JSONArray recentErrors = new JSONArray();
            
            int startIndex = Math.max(0, allErrors.length() - count);
            for (int i = startIndex; i < allErrors.length(); i++) {
                recentErrors.put(allErrors.get(i));
            }
            
            return recentErrors;
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}