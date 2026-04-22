package com.pdf.ai.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pdf.ai.model.Conversation;
import com.pdf.ai.model.ConversationMessage;
import com.pdf.ai.model.LLMModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConversationManager {
    private static final String TAG = "ConversationManager";
    private static final String PREFS_NAME = "PdfAIConversations";
    private static final String KEY_CONVERSATIONS = "conversations";
    private static final String KEY_SELECTED_PROVIDER = "selectedProvider";
    private static final String KEY_SELECTED_MODEL = "selectedModel";
    
    private final SharedPreferences prefs;
    private List<Conversation> conversations;
    private Conversation currentConversation;
    
    public ConversationManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        conversations = new ArrayList<>();
        loadConversations();
    }
    
    public List<Conversation> getConversations() {
        return conversations;
    }
    
    public Conversation createConversation(String provider, String modelId) {
        Conversation conv = new Conversation("New Chat", provider, modelId);
        conversations.add(0, conv);
        currentConversation = conv;
        saveConversations();
        return conv;
    }
    
    public Conversation getCurrentConversation() {
        return currentConversation;
    }
    
    public void setCurrentConversation(Conversation conversation) {
        this.currentConversation = conversation;
    }
    
    public void setCurrentConversationById(String id) {
        for (Conversation conv : conversations) {
            if (conv.getId().equals(id)) {
                currentConversation = conv;
                return;
            }
        }
    }
    
    public void deleteConversation(String id) {
        conversations.removeIf(conv -> conv.getId().equals(id));
        if (currentConversation != null && currentConversation.getId().equals(id)) {
            currentConversation = null;
        }
        saveConversations();
    }
    
    public void addMessageToCurrentConversation(ConversationMessage message) {
        if (currentConversation != null) {
            currentConversation.addMessage(message);
            saveConversations();
        }
    }
    
    public void updateCurrentConversationTitle(String title) {
        if (currentConversation != null) {
            currentConversation.setTitle(title);
            currentConversation.touch();
            saveConversations();
        }
    }
    
    public void saveConversations() {
        try {
            JSONArray array = new JSONArray();
            for (Conversation conv : conversations) {
                array.put(conversationToJson(conv));
            }
            prefs.edit().putString(KEY_CONVERSATIONS, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save conversations", e);
        }
    }
    
    public void loadConversations() {
        conversations.clear();
        String json = prefs.getString(KEY_CONVERSATIONS, null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    conversations.add(jsonToConversation(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse conversations", e);
            }
        }
    }
    
    private JSONObject conversationToJson(Conversation conv) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", conv.getId());
        obj.put("title", conv.getTitle());
        obj.put("provider", conv.getProvider());
        obj.put("modelId", conv.getModelId());
        obj.put("createdAt", conv.getCreatedAt());
        obj.put("updatedAt", conv.getUpdatedAt());
        obj.put("thinkingEnabled", conv.isThinkingEnabled());
        obj.put("searchEnabled", conv.isSearchEnabled());
        if (conv.getCustomPrompt() != null) {
            obj.put("customPrompt", conv.getCustomPrompt());
        }
        
        JSONArray messagesArray = new JSONArray();
        for (ConversationMessage msg : conv.getMessages()) {
            messagesArray.put(messageToJson(msg));
        }
        obj.put("messages", messagesArray);
        
        return obj;
    }
    
    private Conversation jsonToConversation(JSONObject obj) throws JSONException {
        Conversation conv = new Conversation();
        conv.setId(obj.getString("id"));
        conv.setTitle(obj.optString("title", "New Chat"));
        conv.setProvider(obj.optString("provider", "DeepInfra"));
        conv.setModelId(obj.optString("modelId", ""));
        conv.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
        conv.setUpdatedAt(obj.optLong("updatedAt", System.currentTimeMillis()));
        conv.setThinkingEnabled(obj.optBoolean("thinkingEnabled", false));
        conv.setSearchEnabled(obj.optBoolean("searchEnabled", false));
        conv.setCustomPrompt(obj.optString("customPrompt", null));
        
        JSONArray messagesArray = obj.optJSONArray("messages");
        if (messagesArray != null) {
            for (int i = 0; i < messagesArray.length(); i++) {
                conv.getMessages().add(jsonToMessage(messagesArray.getJSONObject(i)));
            }
        }
        
        return conv;
    }
    
    private JSONObject messageToJson(ConversationMessage msg) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("role", msg.getRole());
        obj.put("content", msg.getContent() != null ? msg.getContent() : "");
        obj.put("timestamp", msg.getTimestamp());
        if (msg.getThinking() != null) {
            obj.put("thinking", msg.getThinking());
        }
        return obj;
    }
    
    private ConversationMessage jsonToMessage(JSONObject obj) throws JSONException {
        ConversationMessage msg = new ConversationMessage();
        msg.setRole(obj.getString("role"));
        msg.setContent(obj.optString("content", ""));
        msg.setTimestamp(obj.optLong("timestamp", System.currentTimeMillis()));
        msg.setThinking(obj.optString("thinking", null));
        return msg;
    }
    
    public void setSelectedProvider(String provider) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, provider).apply();
    }
    
    public String getSelectedProvider() {
        return prefs.getString(KEY_SELECTED_PROVIDER, "DeepInfra");
    }
    
    public void setSelectedModel(String modelId) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply();
    }
    
    public String getSelectedModel() {
        return prefs.getString(KEY_SELECTED_MODEL, "Qwen/Qwen2.5-Coder-32B-Instruct");
    }
}