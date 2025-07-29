package com.pdf.ai;

import android.content.Context;
import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.ChatMessage;
import com.pdf.ai.OutlineData;
import com.pdf.ai.GeminiApiClient;
import com.pdf.ai.QwenApiClient;
import com.pdf.ai.ModelSelectorComponent;
import com.pdf.ai.PdfGenerator;
import com.pdf.ai.DialogManager;
import com.pdf.ai.PreferencesManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.TextView;

public class ChatActivity extends AppCompatActivity implements
        MessageAdapter.OnOutlineActionListener,
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
    private GeminiApiClient geminiApiClient;
    private QwenApiClient qwenApiClient;
    private ModelSelectorComponent modelSelectorComponent;
    private PdfGenerator pdfGenerator;
    private DialogManager dialogManager;

    private OutlineData currentOutlineData;
    private boolean isGeneratingPdf = false;
    private List<String> currentPdfSectionsContent;
    private String currentChatId;
    private String currentParentId;

    // Executor Service for background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        preferencesManager = new PreferencesManager(this);
        geminiApiClient = new GeminiApiClient();
        qwenApiClient = new QwenApiClient();
        pdfGenerator = new PdfGenerator(this);
        dialogManager = new DialogManager(this, preferencesManager);

        geminiApiKey = preferencesManager.getGeminiApiKey();
        selectedModel = preferencesManager.getSelectedModel();

        initViews();
        setupModelSelector();
        setupRecyclerView();
        setupInputListeners();
        
        loadChatHistory();
        updateEmptyState();

        // Initialize the ExecutorService
        executorService = Executors.newSingleThreadExecutor();
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
        messageAdapter = new MessageAdapter(this, chatMessages, this, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void setupInputListeners() {
        // Text change listener for send button state
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                sendButton.setEnabled(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendUserMessage(message);
                messageEditText.setText("");
            }
        });

        // Tune button click listener
        tuneButton.setOnClickListener(v -> showTuneSettings());
    }

    @Override
    public void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo) {
        selectedModel = modelId;
        preferencesManager.setSelectedModel(selectedModel);
        
        // Update model selector view
        View selectorView = modelSelectorContainer.getChildAt(0);
        if (selectorView != null) {
            modelSelectorComponent.updateSelectorView(selectorView, modelId);
        }
        
        // Reset features if model doesn't support them
        if (!modelInfo.supportsThinking) {
            thinkingEnabled = false;
        }
        if (!modelInfo.supportsWebSearch) {
            webSearchEnabled = false;
        }
    }

    private void showTuneSettings() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_tune_settings, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        MaterialSwitch thinkingSwitch = bottomSheetView.findViewById(R.id.switch_thinking);
        MaterialSwitch webSearchSwitch = bottomSheetView.findViewById(R.id.switch_web_search);
        TextView compatibilityNote = bottomSheetView.findViewById(R.id.tv_compatibility_note);

        // Get current model info
        Map<String, QwenApiClient.ModelInfo> models = QwenApiClient.getAvailableModels();
        QwenApiClient.ModelInfo currentModel = models.get(selectedModel);

        if (currentModel != null) {
            // Enable/disable switches based on model capabilities
            thinkingSwitch.setEnabled(currentModel.supportsThinking);
            webSearchSwitch.setEnabled(currentModel.supportsWebSearch);
            
            // Set current values
            thinkingSwitch.setChecked(thinkingEnabled && currentModel.supportsThinking);
            webSearchSwitch.setChecked(webSearchEnabled && currentModel.supportsWebSearch);

            // Update compatibility note
            if (!currentModel.supportsThinking && !currentModel.supportsWebSearch) {
                compatibilityNote.setText("Note: " + currentModel.displayName + " doesn't support advanced features");
            } else if (!currentModel.supportsThinking) {
                compatibilityNote.setText("Note: " + currentModel.displayName + " doesn't support thinking mode");
            } else if (!currentModel.supportsWebSearch) {
                compatibilityNote.setText("Note: " + currentModel.displayName + " doesn't support web search");
            } else {
                compatibilityNote.setVisibility(View.GONE);
            }

            // Set listeners
            thinkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                thinkingEnabled = isChecked && currentModel.supportsThinking;
            });

            webSearchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                webSearchEnabled = isChecked && currentModel.supportsWebSearch;
            });
        } else {
            // Handle the case where the model is not found
            thinkingSwitch.setEnabled(false);
            webSearchSwitch.setEnabled(false);
            thinkingSwitch.setChecked(false);
            webSearchSwitch.setChecked(false);
            compatibilityNote.setText("Note: Selected model not found. Features disabled.");
        }

        bottomSheetDialog.show();
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
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service when the activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    private void addWelcomeMessage() {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_SUGGESTIONS, null, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();
    }

    private void sendUserMessage(String message) {
        // Remove suggestions if present
        if (!chatMessages.isEmpty() && chatMessages.get(0).getType() == ChatMessage.TYPE_SUGGESTIONS) {
            chatMessages.remove(0);
            messageAdapter.notifyItemRemoved(0);
        }

        chatMessages.add(new ChatMessage(ChatMessage.TYPE_USER, message, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();

        // Check if it's a Qwen model
        Map<String, QwenApiClient.ModelInfo> models = QwenApiClient.getAvailableModels();
        QwenApiClient.ModelInfo modelInfo = models.get(selectedModel);
        
        if (modelInfo != null && modelInfo.isQwenModel) {
            handleQwenMessage(message);
        } else {
            startOutlineGeneration(message);
        }
    }

    private void handleQwenMessage(String message) {
        if (currentChatId == null) {
            // Create new chat first
            qwenApiClient.createNewChat("New Chat", new String[]{selectedModel}, new QwenApiClient.NewChatCallback() {
                @Override
                public void onSuccess(String chatId) {
                    currentChatId = chatId;
                    sendQwenCompletion(message);
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Failed to create chat: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            sendQwenCompletion(message);
        }
    }

    private void sendQwenCompletion(String message) {
        runOnUiThread(() -> showProgressMessage("Sending request...", 0));
        
        qwenApiClient.sendCompletion(currentChatId, selectedModel, message, currentParentId, 
                thinkingEnabled, webSearchEnabled, new QwenApiClient.QwenApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    removeProgressMessage();
                    try {
                        JSONObject responseObj = new JSONObject(response);
                        String answer = responseObj.optString("answer", "");
                        String thinking = responseObj.optString("thinking", "");
                        String webSearch = responseObj.optString("web_search", "");
                        
                        // Create AI response message
                        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_AI, answer, null, null);
                        if (!thinking.isEmpty()) {
                            aiMessage.setThinkingContent(thinking);
                        }
                        if (!webSearch.isEmpty()) {
                            aiMessage.setWebSearchContent(webSearch);
                        }
                        
                        chatMessages.add(aiMessage);
                        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                        saveChatHistory();
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    removeProgressMessage();
                    Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onStreamUpdate(String partialResponse) {
                runOnUiThread(() -> {
                    // Could be used for streaming updates in the future
                });
            }
        });
    }

    private void showProgressMessage(String status, int progressValue) {
        if (chatMessages.isEmpty() || chatMessages.get(chatMessages.size() - 1).getType() != ChatMessage.TYPE_PROGRESS) {
            chatMessages.add(new ChatMessage(ChatMessage.TYPE_PROGRESS, null, status, progressValue, null));
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        } else {
            updateProgressMessage(status, progressValue);
        }
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();
    }

    private void updateProgressMessage(String status, int progressValue) {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                lastMessage.setProgressStatus(status);
                lastMessage.setProgressValue(progressValue);
                messageAdapter.notifyItemChanged(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        }
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

    private void startOutlineGeneration(String userPrompt) {
        showProgressMessage("Sending request...", 0);
        callGeminiForOutline(userPrompt);
    }

    private void callGeminiForOutline(String userPrompt) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            Toast.makeText(this, "Gemini API Key is not set. Please set it in settings.", Toast.LENGTH_LONG).show();
            updateProgressMessage("Error: API Key missing.", 0);
            return;
        }

        geminiApiClient.generateOutline(userPrompt, geminiApiKey, selectedModel, new GeminiApiClient.GeminiApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String outlineText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        String cleanedJson = cleanJson(outlineText);
                        JSONObject outlineJson = new JSONObject(cleanedJson);
                        String pdfTitle = outlineJson.getString("title");
                        JSONArray sectionsArray = outlineJson.getJSONArray("sections");
                        List<String> sections = new ArrayList<>();
                        for (int i = 0; i < sectionsArray.length(); i++) {
                            sections.add(sectionsArray.getJSONObject(i).getString("section_title"));
                        }
                        currentOutlineData = new OutlineData(pdfTitle, sections);

                        removeProgressMessage();
                        showOutlineInChat(currentOutlineData);
                    } catch (JSONException e) {
                        updateProgressMessage("Failed to parse outline: " + e.getMessage(), 0);
                        Log.e("GeminiAPI", "Failed to parse outline JSON", e);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    updateProgressMessage("Failed to generate outline: " + error, 0);
                    Log.e("GeminiAPI", "Outline generation failed", new Exception(error));
                });
            }
        });
    }

    private void showOutlineInChat(OutlineData outlineData) {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_OUTLINE, null, null, outlineData));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();
        saveChatHistory();
    }

    // Placeholder methods for interface implementation
    @Override
    public void onApproveOutline(OutlineData outlineData) {
        // Implementation for PDF generation...
    }

    @Override
    public void onDiscardOutline(int position) {
        // Remove the outline message from the chat
        if (position >= 0 && position < chatMessages.size()) {
            chatMessages.remove(position);
            messageAdapter.notifyItemRemoved(position);
            updateEmptyState();
            saveChatHistory();
        }
    }

    @Override
    public void onSuggestionClick(String suggestion) {
        messageEditText.setText(suggestion);
        sendUserMessage(suggestion);
    }

    // Helper methods
    private String cleanJson(String jsonString) {
        return jsonString.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
    }

    private void loadChatHistory() {
        // Implementation for loading chat history...
    }

    private void saveChatHistory() {
        // Implementation for saving chat history...
    }
}
