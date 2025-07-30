package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity
    implements MessageAdapter.OnSuggestionClickListener, ModelSelectorComponent.OnModelSelectedListener {

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
    private GeminiApiClient geminiApiClient;
    private QwenApiClient qwenApiClient;
    private ModelSelectorComponent modelSelectorComponent;
    private DialogManager dialogManager;
    private InstructionManager instructionManager;
    private ConversationManager conversationManager;

    private String currentChatId;
    private String currentParentId;
    private ConversationManager.Conversation currentConversation;

    private ExecutorService executorService;

    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        preferencesManager = new PreferencesManager(this);
        geminiApiClient = new GeminiApiClient();
        qwenApiClient = new QwenApiClient();
        dialogManager = new DialogManager(this, preferencesManager);
        instructionManager = new InstructionManager(this);
        conversationManager = new ConversationManager(this);

        geminiApiKey = preferencesManager.getGeminiApiKey();
        selectedModel = preferencesManager.getSelectedModel();

        initViews();
        setupModelSelector();
        setupRecyclerView();
        setupInputListeners();

        initializeConversation();
        updateEmptyState();

        executorService = Executors.newSingleThreadExecutor();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        tuneButton = findViewById(R.id.btn_tune);
        modelSelectorContainer = findViewById(R.id.fl_model_selector_container);
        emptyStateContainer = findViewById(R.id.empty_state_container);
    }

    private void setupModelSelector() {
        modelSelectorComponent = new ModelSelectorComponent(this, this);
        modelSelectorComponent.setSelectedModel(selectedModel);
        View selectorView = modelSelectorComponent.createSelectorView();
        modelSelectorContainer.addView(selectorView);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, chatMessages, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void setupInputListeners() {
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.toString().trim().length() > 0);
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

    @Override
    public void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo) {
        selectedModel = modelId;
        preferencesManager.setSelectedModel(selectedModel);

        View selectorView = modelSelectorContainer.getChildAt(0);
        if (selectorView != null) {
            modelSelectorComponent.updateSelectorView(selectorView, modelId);
        }

        if (!modelInfo.supportsThinking) thinkingEnabled = false;
        if (!modelInfo.supportsWebSearch) webSearchEnabled = false;
    }

    private void showTuneSettings() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_tune_settings, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        MaterialSwitch thinkingSwitch = bottomSheetView.findViewById(R.id.switch_thinking);
        MaterialSwitch webSearchSwitch = bottomSheetView.findViewById(R.id.switch_web_search);
        TextView compatibilityNote = bottomSheetView.findViewById(R.id.tv_compatibility_note);

        Map<String, QwenApiClient.ModelInfo> models = QwenApiClient.getAvailableModels();
        QwenApiClient.ModelInfo currentModel = models.get(selectedModel);

        if (currentModel != null) {
            thinkingSwitch.setEnabled(currentModel.supportsThinking);
            webSearchSwitch.setEnabled(currentModel.supportsWebSearch);
            thinkingSwitch.setChecked(thinkingEnabled && currentModel.supportsThinking);
            webSearchSwitch.setChecked(webSearchEnabled && currentModel.supportsWebSearch);

            // ... (rest of the tune settings logic is fine)
        }
        bottomSheetDialog.show();
    }

    private void updateEmptyState() {
        emptyStateContainer.setVisibility(chatMessages.isEmpty() ? View.VISIBLE : View.GONE);
        chatRecyclerView.setVisibility(chatMessages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    private void addWelcomeMessage() {
        if (chatMessages.isEmpty()) {
            chatMessages.add(new ChatMessage(ChatMessage.TYPE_SUGGESTIONS));
            messageAdapter.notifyItemInserted(0);
            chatRecyclerView.scrollToPosition(0);
            updateEmptyState();
        }
    }

    private void sendUserMessage(String message) {
        if (!chatMessages.isEmpty() && chatMessages.get(0).getType() == ChatMessage.TYPE_SUGGESTIONS) {
            chatMessages.remove(0);
            messageAdapter.notifyItemRemoved(0);
        }

        ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, message);
        chatMessages.add(userMessage);
        saveConversationMessage(userMessage);
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();

        handleQwenMessage(message);
    }

    private void handleQwenMessage(String message) {
        if (currentChatId == null) {
            qwenApiClient.createNewChat(
                "New Chat",
                new String[] { selectedModel },
                new QwenApiClient.NewChatCallback() {
                    @Override
                    public void onSuccess(String chatId) {
                        currentChatId = chatId;
                        conversationManager.setQwenChatId(chatId);
                        sendQwenCompletion(message);
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to create chat: " + error, Toast.LENGTH_SHORT).show());
                    }
                }
            );
        } else {
            sendQwenCompletion(message);
        }
    }

    private void sendQwenCompletion(String message) {
        showProgressMessage("Sending request...", 0);
        String enhancedMessage = instructionManager.getGeneralChatInstructions(message, thinkingEnabled, webSearchEnabled);

        // Add a placeholder for the AI's response
        final ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, "");
        runOnUiThread(() -> {
            chatMessages.add(aiMessage);
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        });

        qwenApiClient.sendCompletion(
            currentChatId,
            selectedModel,
            enhancedMessage,
            currentParentId,
            thinkingEnabled,
            webSearchEnabled,
            new QwenApiClient.QwenApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        removeProgressMessage();
                        try {
                            JSONObject responseObj = new JSONObject(response);
                            String answer = responseObj.optString("answer", "");
                            String thinking = responseObj.optString("thinking", "");
                            String webSearch = responseObj.optString("web_search", "");

                            aiMessage.setMessage(answer); // Set the final message
                            if (!thinking.isEmpty()) aiMessage.setThinkingContent(thinking);
                            if (!webSearch.isEmpty()) aiMessage.setWebSearchContent(webSearch);

                            int messagePosition = chatMessages.indexOf(aiMessage);
                            if (messagePosition != -1) {
                                messageAdapter.notifyItemChanged(messagePosition);
                            }
                            saveConversationMessage(aiMessage);
                        } catch (JSONException e) {
                            Toast.makeText(ChatActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        removeProgressMessage();
                        int messagePosition = chatMessages.indexOf(aiMessage);
                        if (messagePosition != -1) {
                            chatMessages.remove(messagePosition);
                            messageAdapter.notifyItemRemoved(messagePosition);
                        }
                        Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onStreamUpdate(String type, String content) {
                    runOnUiThread(() -> {
                        if (type.equals("text")) {
                            aiMessage.setMessage(aiMessage.getMessage() + content);
                            int messagePosition = chatMessages.indexOf(aiMessage);
                            if (messagePosition != -1) {
                                messageAdapter.notifyItemChanged(messagePosition);
                                chatRecyclerView.scrollToPosition(messagePosition);
                            }
                        }
                    });
                }
            }
        );
    }

    private void showProgressMessage(String status, int progressValue) {
        runOnUiThread(() -> {
            if (chatMessages.isEmpty() || chatMessages.get(chatMessages.size() - 1).getType() != ChatMessage.TYPE_PROGRESS) {
                chatMessages.add(new ChatMessage(ChatMessage.TYPE_PROGRESS, null, status, progressValue));
                messageAdapter.notifyItemInserted(chatMessages.size() - 1);
            } else {
                updateProgressMessage(status, progressValue);
            }
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            updateEmptyState();
        });
    }

    private void updateProgressMessage(String status, int progressValue) {
        runOnUiThread(() -> {
            if (!chatMessages.isEmpty()) {
                ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
                if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                    lastMessage.setProgressStatus(status);
                    lastMessage.setProgressValue(progressValue);
                    messageAdapter.notifyItemChanged(chatMessages.size() - 1);
                }
            }
        });
    }

    private void removeProgressMessage() {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                chatMessages.remove(chatMessages.size() - 1);
                messageAdapter.notifyItemRemoved(chatMessages.size());
            }
        }
        updateEmptyState();
    }

    @Override
    public void onSuggestionClick(String suggestion) {
        messageEditText.setText(suggestion);
        sendUserMessage(suggestion);
    }

    private void initializeConversation() {
        Intent intent = getIntent();
        boolean isNewChat = intent.getBooleanExtra("is_new_chat", false);
        String conversationId = intent.getStringExtra("conversationId");

        if (isNewChat) {
            startNewConversation();
        } else if (conversationId != null) {
            loadExistingConversation(conversationId);
        } else {
            String lastConversationId = conversationManager.getCurrentConversationId();
            if (lastConversationId != null) {
                loadExistingConversation(lastConversationId);
            } else {
                startNewConversation();
            }
        }
    }

    private void loadExistingConversation(String conversationId) {
        currentConversation = conversationManager.loadConversation(conversationId);
        if (currentConversation != null) {
            conversationManager.saveCurrentConversationId(conversationId);
            loadConversationMessages();
            if (currentConversation.isQwenConversation) {
                currentChatId = conversationManager.getQwenChatId();
                currentParentId = conversationManager.getQwenParentId();
            }
        } else {
            startNewConversation();
        }
    }

    private void startNewConversation() {
        conversationManager.clearCurrentConversation();
        currentConversation = conversationManager.startNewConversation("New Chat", selectedModel);
        chatMessages.clear();
        if (messageAdapter != null) {
            messageAdapter.notifyDataSetChanged();
        }
        currentChatId = null;
        currentParentId = null;
        addWelcomeMessage();
    }

    private void loadConversationMessages() {
        if (currentConversation != null && currentConversation.messages != null) {
            chatMessages.clear();
            chatMessages.addAll(currentConversation.messages);
            messageAdapter.notifyDataSetChanged();
            if (!chatMessages.isEmpty()) {
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            } else {
                addWelcomeMessage();
            }
        }
    }

    private void saveConversationMessage(ChatMessage message) {
        if (currentConversation != null) {
            currentConversation.addMessage(message);
            conversationManager.saveConversation(currentConversation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
