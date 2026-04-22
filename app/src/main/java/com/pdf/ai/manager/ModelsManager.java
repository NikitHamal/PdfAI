package com.pdf.ai.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pdf.ai.model.LLMModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelsManager {
    private static final String TAG = "ModelsManager";
    private static final String PREFS_NAME = "PdfAIModels";
    private static final String KEY_CACHED_MODELS = "cachedModels";
    private static final String KEY_CACHE_TIMESTAMP = "cacheTimestamp";
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000;
    
    private static ModelsManager instance;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final Map<String, List<LLMModel>> modelsCache;
    private final Map<String, List<LLMModel>> staticModels;
    
    public interface ModelsFetchCallback {
        void onSuccess(Map<String, List<LLMModel>> models);
        void onError(String error);
    }
    
    private ModelsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
        modelsCache = new HashMap<>();
        staticModels = new HashMap<>();
        initializeStaticModels();
        loadCachedModels();
    }
    
    public static synchronized ModelsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModelsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private void initializeStaticModels() {
        List<LLMModel> deepinfraModels = new ArrayList<>();
        String[][] deepinfraStatic = {
            {"Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B", "32"},
            {"Qwen/Qwen2.5-72B-Instruct", "Qwen 2.5 72B", "72"},
            {"meta-llama/Meta-Llama-3.1-8B-Instruct", "Llama 3.1 (8B)", "8"},
            {"meta-llama/Meta-Llama-3.3-70B-Instruct", "Llama 3.3 (70B)", "70"},
            {"deepseek-ai/DeepSeek-V3.2", "DeepSeek V3.2", "128"},
            {"zai-org/GLM-4.7-Flash", "GLM 4.7 Flash", "128"}
        };
        for (String[] m : deepinfraStatic) {
            LLMModel model = new LLMModel(m[0], m[1], "DeepInfra");
            model.setContextWindow(Integer.parseInt(m[2]) * 1024);
            Map<String, Boolean> caps = new HashMap<>();
            caps.put("chat", true);
            caps.put("stream", true);
            caps.put("vision", false);
            caps.put("reasoning", m[0].contains("DeepSeek"));
            caps.put("tools", true);
            model.setCapabilities(caps);
            model.setInputCost(0);
            model.setOutputCost(0);
            deepinfraModels.add(model);
        }
        staticModels.put("DeepInfra", deepinfraModels);
        
        List<LLMModel> qwenModels = new ArrayList<>();
        String[][] qwenStatic = {
            {"qwen3.6-plus", "Qwen 3.6 Plus", "128"},
            {"qwen3-235b-a22b", "Qwen 3 235B A22B", "128"},
            {"qwen2.5-max", "Qwen 2.5 Max", "128"},
            {"qwen2.5-72b-instruct", "Qwen 2.5 72B", "128"},
            {"qwen2.5-32b-instruct", "Qwen 2.5 32B", "128"}
        };
        for (String[] m : qwenStatic) {
            LLMModel model = new LLMModel(m[0], m[1], "Qwen");
            model.setContextWindow(Integer.parseInt(m[2]) * 1024);
            Map<String, Boolean> caps = new HashMap<>();
            caps.put("chat", true);
            caps.put("stream", true);
            caps.put("vision", m[0].contains("vl") || m[0].contains("vision"));
            caps.put("reasoning", true);
            caps.put("tools", true);
            model.setCapabilities(caps);
            model.setInputCost(0);
            model.setOutputCost(0);
            qwenModels.add(model);
        }
        staticModels.put("Qwen", qwenModels);
    }
    
    private void loadCachedModels() {
        String cached = prefs.getString(KEY_CACHED_MODELS, null);
        long timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0);
        
        if (cached != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            try {
                JSONObject obj = new JSONObject(cached);
                for (String provider : new String[]{"DeepInfra", "Qwen", "Gemini"}) {
                    if (obj.has(provider)) {
                        JSONArray arr = obj.getJSONArray(provider);
                        List<LLMModel> models = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            models.add(parseModel(arr.getJSONObject(i), provider));
                        }
                        modelsCache.put(provider, models);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to load cached models", e);
            }
        }
    }
    
    private LLMModel parseModel(JSONObject obj, String provider) throws JSONException {
        LLMModel model = new LLMModel();
        model.setId(obj.getString("id"));
        model.setName(obj.optString("name", model.getId()));
        model.setProvider(provider);
        model.setContextWindow(obj.optInt("contextWindow", 32768));
        model.setInputCost(obj.optDouble("inputCost", 0));
        model.setOutputCost(obj.optDouble("outputCost", 0));
        
        JSONObject caps = obj.optJSONObject("capabilities");
        if (caps != null) {
            Map<String, Boolean> capabilities = new HashMap<>();
            capabilities.put("chat", caps.optBoolean("chat", true));
            capabilities.put("stream", caps.optBoolean("stream", true));
            capabilities.put("vision", caps.optBoolean("vision", false));
            capabilities.put("reasoning", caps.optBoolean("reasoning", false));
            capabilities.put("tools", caps.optBoolean("tools", true));
            model.setCapabilities(capabilities);
        }
        
        return model;
    }
    
    public void getModels(ModelsFetchCallback callback) {
        executor.execute(() -> {
            try {
                Map<String, List<LLMModel>> result = new HashMap<>();
                
                if (!modelsCache.isEmpty()) {
                    result.putAll(modelsCache);
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                    return;
                }
                
                result.put("DeepInfra", staticModels.get("DeepInfra"));
                result.put("Qwen", staticModels.get("Qwen"));
                
                modelsCache.put("DeepInfra", staticModels.get("DeepInfra"));
                modelsCache.put("Qwen", staticModels.get("Qwen"));
                
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting models", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    public void fetchModelsAsync(ModelsFetchCallback callback, boolean forceRefresh) {
        if (!forceRefresh && !modelsCache.isEmpty()) {
            getModels(callback);
            return;
        }
        
        executor.execute(() -> {
            modelsCache.put("DeepInfra", staticModels.get("DeepInfra"));
            modelsCache.put("Qwen", staticModels.get("Qwen"));
            
            if (callback != null) {
                Map<String, List<LLMModel>> result = new HashMap<>(modelsCache);
                callback.onSuccess(result);
            }
        });
    }
    
    public List<LLMModel> getModelsForProvider(String provider) {
        if (modelsCache.containsKey(provider)) {
            return modelsCache.get(provider);
        }
        return staticModels.getOrDefault(provider, new ArrayList<>());
    }
    
    public LLMModel getModelById(String provider, String modelId) {
        List<LLMModel> models = getModelsForProvider(provider);
        for (LLMModel model : models) {
            if (model.getId().equals(modelId)) {
                return model;
            }
        }
        return null;
    }
    
    public void cacheModels(String provider, List<LLMModel> models) {
        modelsCache.put(provider, models);
        saveToCache();
    }
    
    private void saveToCache() {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, List<LLMModel>> entry : modelsCache.entrySet()) {
                JSONArray arr = new JSONArray();
                for (LLMModel model : entry.getValue()) {
                    arr.put(modelToJson(model));
                }
                obj.put(entry.getKey(), arr);
            }
            prefs.edit()
                .putString(KEY_CACHED_MODELS, obj.toString())
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save models cache", e);
        }
    }
    
    private JSONObject modelToJson(LLMModel model) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", model.getId());
        obj.put("name", model.getName());
        obj.put("contextWindow", model.getContextWindow());
        obj.put("inputCost", model.getInputCost());
        obj.put("outputCost", model.getOutputCost());
        
        if (model.getCapabilities() != null) {
            JSONObject caps = new JSONObject();
            for (Map.Entry<String, Boolean> entry : model.getCapabilities().entrySet()) {
                caps.put(entry.getKey(), entry.getValue());
            }
            obj.put("capabilities", caps);
        }
        
        return obj;
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}