package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.ChatMessage;
import com.pdf.ai.QwenApiClient;
import com.pdf.ai.ModelSelectorComponent;
import com.pdf.ai.PreferencesManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.Menu;

public class ChatActivity extends AppCompatActivity implements
        MessageAdapter.OnSuggestionClickListener,
        ModelSelectorComponent.OnModelSelectedListener {

    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessages;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton tuneButton;
    private FrameLayout modelSelectorContainer;
    private LinearLayout emptyStateContainer;

    private String geminiApiKey;
    private String selectedModel = "qwen3-235b-a22b"; // Default to Qwen model
    private boolean thinkingEnabled = false;
    private boolean webSearchEnabled = false;

    private PreferencesManager preferencesManager;
    private QwenApiClient qwenApiClient;
    private ModelSelectorComponent modelSelectorComponent;
    private ConversationManager conversationManager;

    private String currentChatId;
    private String currentParentId;
    private ConversationManager.Conversation currentConversation;
    private String conversationId;

    // Executor Service for background tasks
    private ExecutorService executorService;

    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        preferencesManager = new PreferencesManager(this);
        qwenApiClient = new QwenApiClient();
        conversationManager = new ConversationManager(this);
        executorService = Executors.newCachedThreadPool();

        initViews();
        setupModelSelector();
        setupRecyclerView();
        setupInputListeners();

        // Handle conversation initialization
        handleConversationIntent();
    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        tuneButton = findViewById(R.id.tune_button);
        modelSelectorContainer = findViewById(R.id.model_selector_container);
        emptyStateContainer = findViewById(R.id.empty_state_container);
    }

    private void setupModelSelector() {
        modelSelectorComponent = new ModelSelectorComponent(this, modelSelectorContainer);
        modelSelectorComponent.setOnModelSelectedListener(this);
        modelSelectorComponent.setSelectedModel(selectedModel);
        View selectorView = modelSelectorComponent.createSelectorView();
        modelSelectorContainer.addView(selectorView);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, chatMessages, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void setupInputListeners() {
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendUserMessage(message);
                messageEditText.setText("");
            }
        });

        tuneButton.setOnClickListener(v -> showTuneSettings());
    }

    private void handleConversationIntent() {
        Intent intent = getIntent();
        boolean isNewConversation = intent.getBooleanExtra("new_conversation", false);

        if (isNewConversation) {
            startNewConversation();
        } else {
            conversationId = intent.getStringExtra("conversation_id");
            if (conversationId != null) {
                loadConversation(conversationId);
            } else {
                startNewConversation();
            }
        }
    }

    private void startNewConversation() {
        currentConversation = conversationManager.startNewConversation("New Chat", selectedModel);
        conversationId = currentConversation.id;
        conversationManager.setCurrentConversationId(conversationId);

        // Clear messages
        chatMessages.clear();
        messageAdapter.notifyDataSetChanged();

        // Set up Qwen conversation
        setupQwenConversation();
    }

    private void loadConversation(String convId) {
        currentConversation = conversationManager.loadConversation(convId);
        if (currentConversation != null) {
            conversationId = convId;
            conversationManager.setCurrentConversationId(conversationId);

            // Load messages
            chatMessages.clear();
            if (currentConversation.messages != null) {
                chatMessages.addAll(currentConversation.messages);
            }
            messageAdapter.notifyDataSetChanged();

            // Set up Qwen conversation if it's a Qwen conversation
            if (currentConversation.isQwenConversation) {
                currentChatId = currentConversation.qwenChatId;
                currentParentId = currentConversation.qwenParentId;
            }

            updateEmptyState();
        } else {
            // Fallback to new conversation if loading fails
            startNewConversation();
        }
    }

    private void setupQwenConversation() {
        // Initialize Qwen conversation
        currentChatId = null;
        currentParentId = null;
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(ChatMessage.TYPE_AI,
            "Hello! I'm your AI assistant. How can I help you today?", null);
        chatMessages.add(welcomeMessage);
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void sendUserMessage(String message) {
        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, message, null);
        chatMessages.add(userMessage);
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        // Save message to conversation
        saveConversationMessage(userMessage);

        // Create streaming AI message
        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, "", null);
        aiMessage.setStreaming(true);
        chatMessages.add(aiMessage);
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        // Send to AI
        handleQwenMessage(message, aiMessage);
    }

    private void handleQwenMessage(String message, ChatMessage aiMessage) {
        if (currentChatId == null) {
            // Start new Qwen conversation
            qwenApiClient.createNewChat("New Chat", new String[]{selectedModel}, new QwenApiClient.NewChatCallback() {
                @Override
                public void onSuccess(String chatId) {
                    currentChatId = chatId;
                    sendQwenCompletion(message, aiMessage);
                }

                @Override
                public void onFailure(String error) {
                    // Remove the streaming message on error
                    if (chatMessages.contains(aiMessage)) {
                        chatMessages.remove(aiMessage);
                        messageAdapter.notifyItemRemoved(chatMessages.size());
                    }
                    showError("Failed to start conversation: " + error);
                }
            });
        } else {
            // Continue existing conversation
            sendQwenCompletion(message, aiMessage);
        }
    }

    private void sendQwenCompletion(String message, ChatMessage aiMessage) {
        qwenApiClient.sendCompletion(currentChatId, selectedModel, message, currentParentId,
                thinkingEnabled, webSearchEnabled, new QwenApiClient.QwenApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            // Finalize the streaming message
                            aiMessage.setStreaming(false);
                            aiMessage.setMessage(response);
                            messageAdapter.notifyItemChanged(chatMessages.indexOf(aiMessage));

                            // Save message to conversation
                            saveConversationMessage(aiMessage);
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            // Remove the streaming message on error
                            if (chatMessages.contains(aiMessage)) {
                                chatMessages.remove(aiMessage);
                                messageAdapter.notifyItemRemoved(chatMessages.size());
                            }
                            showError("Failed to get response: " + error);
                        });
                    }

                    @Override
                    public void onStreamUpdate(String type, String content) {
                        runOnUiThread(() -> {
                            // Update streaming content
                            if (chatMessages.contains(aiMessage)) {
                                aiMessage.appendStreamedContent(content);
                                messageAdapter.notifyItemChanged(chatMessages.indexOf(aiMessage));
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                            }
                        });
                    }
                });
    }

    private void addAiMessage(String response) {
        try {
            // Parse the JSON response
            JSONObject responseJson = new JSONObject(response);
            String answer = responseJson.optString("answer", response);
            String thinking = responseJson.optString("thinking", "");
            String webSearch = responseJson.optString("web_search", "");

            ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, answer, null);

            // Set thinking and web search content if available
            if (!thinking.isEmpty()) {
                aiMessage.setThinkingContent(thinking);
            }
            if (!webSearch.isEmpty()) {
                aiMessage.setWebSearchContent(webSearch);
            }

            chatMessages.add(aiMessage);
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

            // Save message to conversation
            saveConversationMessage(aiMessage);
        } catch (JSONException e) {
            // Fallback to treating response as plain text
            ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, response, null);
            chatMessages.add(aiMessage);
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            saveConversationMessage(aiMessage);
        }
    }

    private void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private void showTuneSettings() {
        View tuneView = getLayoutInflater().inflate(R.layout.bottom_sheet_tune_settings, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(tuneView);

        MaterialSwitch thinkingSwitch = tuneView.findViewById(R.id.thinking_switch);
        MaterialSwitch webSearchSwitch = tuneView.findViewById(R.id.web_search_switch);

        thinkingSwitch.setChecked(thinkingEnabled);
        webSearchSwitch.setChecked(webSearchEnabled);

        thinkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            thinkingEnabled = isChecked;
            preferencesManager.saveThinkingEnabled(isChecked);
        });

        webSearchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            webSearchEnabled = isChecked;
            preferencesManager.saveWebSearchEnabled(isChecked);
        });

        dialog.show();
    }

    private void updateEmptyState() {
        if (chatMessages.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            chatRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            chatRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo) {
        selectedModel = modelId;
        preferencesManager.saveSelectedModel(modelId);
        Toast.makeText(this, "Model changed to: " + modelInfo.displayName, Toast.LENGTH_SHORT).show();

        // Update the selector view
        View selectorView = modelSelectorContainer.getChildAt(0);
        if (selectorView != null) {
            modelSelectorComponent.updateSelectorView(selectorView, modelId);
        }
    }

    @Override
    public void onSuggestionClick(String suggestion) {
        messageEditText.setText(suggestion);
        messageEditText.requestFocus();
    }

    private void saveConversationMessage(ChatMessage message) {
        if (currentConversation != null) {
            currentConversation.addMessage(message);
            conversationManager.saveConversation(currentConversation);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
