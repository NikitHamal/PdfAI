package com.chat.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ConversationManager handles local conversation storage and Qwen conversation ID management
 */
public class ConversationManager {
    
    private static final String TAG = "ConversationManager";
    private static final String PREFS_NAME = "conversations_prefs";
    private static final String KEY_CURRENT_CONVERSATION_ID = "current_conversation_id";
    private static final String KEY_QWEN_CHAT_ID = "qwen_chat_id";
    private static final String KEY_QWEN_PARENT_ID = "qwen_parent_id";
    private static final String CONVERSATIONS_DIR = "conversations";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final File conversationsDir;
    
    public ConversationManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        // Create conversations directory
        this.conversationsDir = new File(context.getFilesDir(), CONVERSATIONS_DIR);
        if (!conversationsDir.exists()) {
            conversationsDir.mkdirs();
        }
    }
    
    public static class Conversation {
        public String id;
        public String title;
        public String modelUsed;
        public long timestamp;
        public long lastModified;
        public List<ChatMessage> messages;
        public String qwenChatId;
        public String qwenParentId;
        public boolean isQwenConversation;
        
        public Conversation() {
            this.id = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.lastModified = timestamp;
            this.messages = new ArrayList<>();
        }
        
        public Conversation(String title, String modelUsed) {
            this();
            this.title = title;
            this.modelUsed = modelUsed;
            this.isQwenConversation = isQwenModel(modelUsed);
        }
        
        private static boolean isQwenModel(String modelId) {
            return modelId != null && (modelId.startsWith("qwen") || modelId.startsWith("qwq") || modelId.startsWith("qvq"));
        }
        
        public void addMessage(ChatMessage message) {
            messages.add(message);
            updateTitle();
            this.lastModified = System.currentTimeMillis();
        }
        
        private void updateTitle() {
            if (messages.isEmpty()) return;
            
            // Use first user message as title if title is generic
            if (title == null || title.equals("New Chat") || title.isEmpty()) {
                for (ChatMessage msg : messages) {
                    if (msg.getType() == ChatMessage.TYPE_USER && !msg.getMessage().isEmpty()) {
                        this.title = msg.getMessage().length() > 50 ? 
                            msg.getMessage().substring(0, 47) + "..." : msg.getMessage();
                        break;
                    }
                }
            }
        }
        
        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(lastModified));
        }
        
        public int getMessageCount() {
            return messages.size();
        }
    }
    
    /**
     * Start a new conversation
     */
    public Conversation startNewConversation(String title, String modelUsed) {
        Conversation conversation = new Conversation(title, modelUsed);
        saveCurrentConversationId(conversation.id);
        Log.d(TAG, "Started new conversation: " + conversation.id + " with model: " + modelUsed);
        return conversation;
    }
    
    /**
     * Get current conversation ID
     */
    public String getCurrentConversationId() {
        return prefs.getString(KEY_CURRENT_CONVERSATION_ID, null);
    }
    
    /**
     * Save current conversation ID
     */
    private void saveCurrentConversationId(String conversationId) {
        prefs.edit().putString(KEY_CURRENT_CONVERSATION_ID, conversationId).apply();
    }
    
    /**
     * Set Qwen chat ID for the current conversation
     */
    public void setQwenChatId(String qwenChatId) {
        prefs.edit().putString(KEY_QWEN_CHAT_ID, qwenChatId).apply();
        Log.d(TAG, "Set Qwen chat ID: " + qwenChatId);
    }
    
    /**
     * Get Qwen chat ID for the current conversation
     */
    public String getQwenChatId() {
        return prefs.getString(KEY_QWEN_CHAT_ID, null);
    }
    
    /**
     * Set Qwen parent ID for the current conversation
     */
    public void setQwenParentId(String parentId) {
        prefs.edit().putString(KEY_QWEN_PARENT_ID, parentId).apply();
        Log.d(TAG, "Set Qwen parent ID: " + parentId);
    }
    
    /**
     * Get Qwen parent ID for the current conversation
     */
    public String getQwenParentId() {
        return prefs.getString(KEY_QWEN_PARENT_ID, null);
    }
    
    /**
     * Save conversation to local storage
     */
    public boolean saveConversation(Conversation conversation) {
        try {
            File conversationFile = new File(conversationsDir, conversation.id + ".json");
            
            // Update Qwen IDs in conversation if they exist
            String qwenChatId = getQwenChatId();
            String qwenParentId = getQwenParentId();
            if (qwenChatId != null) {
                conversation.qwenChatId = qwenChatId;
            }
            if (qwenParentId != null) {
                conversation.qwenParentId = qwenParentId;
            }
            
            try (FileWriter writer = new FileWriter(conversationFile)) {
                gson.toJson(conversation, writer);
                Log.d(TAG, "Saved conversation: " + conversation.id + " to " + conversationFile.getAbsolutePath());
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save conversation: " + conversation.id, e);
            return false;
        }
    }
    
    /**
     * Load conversation from local storage
     */
    public Conversation loadConversation(String conversationId) {
        try {
            File conversationFile = new File(conversationsDir, conversationId + ".json");
            if (!conversationFile.exists()) {
                Log.w(TAG, "Conversation file not found: " + conversationFile.getAbsolutePath());
                return null;
            }
            
            try (FileReader reader = new FileReader(conversationFile)) {
                Conversation conversation = gson.fromJson(reader, Conversation.class);
                
                // Restore Qwen IDs to preferences if this conversation has them
                if (conversation.isQwenConversation) {
                    if (conversation.qwenChatId != null) {
                        setQwenChatId(conversation.qwenChatId);
                    }
                    if (conversation.qwenParentId != null) {
                        setQwenParentId(conversation.qwenParentId);
                    }
                }
                
                Log.d(TAG, "Loaded conversation: " + conversationId);
                return conversation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load conversation: " + conversationId, e);
            return null;
        }
    }
    
    /**
     * Get all saved conversations
     */
    public List<Conversation> getAllConversations() {
        List<Conversation> conversations = new ArrayList<>();
        
        if (!conversationsDir.exists()) {
            return conversations;
        }
        
        File[] files = conversationsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String conversationId = file.getName().replace(".json", "");
                Conversation conversation = loadConversation(conversationId);
                if (conversation != null) {
                    conversations.add(conversation);
                }
            }
        }
        
        // Sort by last modified (newest first)
        conversations.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
        
        Log.d(TAG, "Loaded " + conversations.size() + " conversations");
        return conversations;
    }
    
    /**
     * Delete a conversation
     */
    public boolean deleteConversation(String conversationId) {
        try {
            File conversationFile = new File(conversationsDir, conversationId + ".json");
            boolean deleted = conversationFile.delete();
            
            // Clear current conversation if it's the one being deleted
            if (conversationId.equals(getCurrentConversationId())) {
                clearCurrentConversation();
            }
            
            Log.d(TAG, "Deleted conversation: " + conversationId + " - " + deleted);
            return deleted;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete conversation: " + conversationId, e);
            return false;
        }
    }
    
    /**
     * Clear current conversation data
     */
    public void clearCurrentConversation() {
        prefs.edit()
                .remove(KEY_CURRENT_CONVERSATION_ID)
                .remove(KEY_QWEN_CHAT_ID)
                .remove(KEY_QWEN_PARENT_ID)
                .apply();
        Log.d(TAG, "Cleared current conversation data");
    }
    
    /**
     * Update conversation with new message
     */
    public boolean updateConversationWithMessage(String conversationId, ChatMessage message) {
        Conversation conversation = loadConversation(conversationId);
        if (conversation != null) {
            conversation.addMessage(message);
            return saveConversation(conversation);
        }
        return false;
    }
    
    /**
     * Get conversation statistics
     */
    public Map<String, Object> getConversationStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Conversation> conversations = getAllConversations();
        
        stats.put("totalConversations", conversations.size());
        
        int qwenCount = 0;
        int geminiCount = 0;
        int totalMessages = 0;
        
        for (Conversation conv : conversations) {
            totalMessages += conv.getMessageCount();
            if (conv.isQwenConversation) {
                qwenCount++;
            } else {
                geminiCount++;
            }
        }
        
        stats.put("qwenConversations", qwenCount);
        stats.put("geminiConversations", geminiCount);
        stats.put("totalMessages", totalMessages);
        stats.put("averageMessagesPerConversation", conversations.size() > 0 ? totalMessages / conversations.size() : 0);
        
        return stats;
    }
}