package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.pdf.ai.MessageAdapter;
import com.pdf.ai.manager.ConversationManager;
import com.pdf.ai.manager.ModelsManager;
import com.pdf.ai.model.Conversation;
import com.pdf.ai.model.ConversationMessage;
import com.pdf.ai.model.LLMModel;
import com.pdf.ai.provider.LLMProvider;
import com.pdf.ai.provider.ProviderFactory;
import com.pdf.ai.ui.interaction.OnOutlineActionListener;
import com.pdf.ai.ui.interaction.OnSuggestionClickListener;
import com.pdf.ai.util.MarkdownParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity implements
        OnOutlineActionListener,
        OnSuggestionClickListener {

    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessages;
    private EditText messageEditText;
    private LinearLayout sendButton;
    private TextView modelNameText;
    private Chip thinkingChip;
    private Chip searchChip;

    private ConversationManager conversationManager;
    private ModelsManager modelsManager;
    private LLMProvider llmProvider;
    private PdfGenerator pdfGenerator;
    private ExecutorService executorService;

    private String conversationId;
    private String selectedProvider;
    private String selectedModelId;
    private boolean thinkingEnabled = false;
    private boolean searchEnabled = false;

    private OutlineData currentOutlineData;
    private boolean isGenerating = false;
    private List<String> currentPdfSectionsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationManager = new ConversationManager(this);
        modelsManager = ModelsManager.getInstance(this);
        pdfGenerator = new PdfGenerator(this);
        executorService = Executors.newSingleThreadExecutor();

        conversationId = getIntent().getStringExtra("conversation_id");
        selectedProvider = conversationManager.getSelectedProvider();
        selectedModelId = conversationManager.getSelectedModel();

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        modelNameText = findViewById(R.id.model_name_text);
        thinkingChip = findViewById(R.id.thinking_chip);
        searchChip = findViewById(R.id.search_chip);
        LinearLayout modelPickerLayout = findViewById(R.id.model_picker_layout);
        ImageView backIcon = findViewById(R.id.back_icon);
        ImageView newChatIcon = findViewById(R.id.new_chat_icon);

        chatMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, chatMessages, this, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        updateModelDisplay();
        loadConversation();

        if (chatMessages.isEmpty()) {
            addWelcomeMessage();
        }

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty() && !isGenerating) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        modelPickerLayout.setOnClickListener(v -> showModelPicker());

        backIcon.setOnClickListener(v -> {
            saveConversation();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        newChatIcon.setOnClickListener(v -> {
            saveConversation();
            Conversation conv = conversationManager.createConversation(selectedProvider, selectedModelId);
            conversationId = conv.getId();
            chatMessages.clear();
            messageAdapter.notifyDataSetChanged();
            addWelcomeMessage();
        });

        thinkingChip.setOnClickListener(v -> {
            thinkingEnabled = !thinkingEnabled;
            thinkingChip.setChecked(thinkingEnabled);
        });

        searchChip.setOnClickListener(v -> {
            searchEnabled = !searchEnabled;
            searchChip.setChecked(searchEnabled);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveConversation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    private void loadConversation() {
        if (conversationId != null) {
            conversationManager.setCurrentConversationById(conversationId);
            Conversation conv = conversationManager.getCurrentConversation();
            if (conv != null) {
                selectedProvider = conv.getProvider();
                selectedModelId = conv.getModelId();
                thinkingEnabled = conv.isThinkingEnabled();
                searchEnabled = conv.isSearchEnabled();
                thinkingChip.setChecked(thinkingEnabled);
                searchChip.setChecked(searchEnabled);

                for (ConversationMessage msg : conv.getMessages()) {
                    int type = msg.isUser() ? ChatMessage.TYPE_USER : ChatMessage.TYPE_AI;
                    chatMessages.add(new ChatMessage(type, msg.getContent(), null, null));
                }
                messageAdapter.notifyDataSetChanged();
                scrollToBottom();
            }
        }
    }

    private void saveConversation() {
        if (conversationId == null) return;

        Conversation conv = conversationManager.getCurrentConversation();
        if (conv == null) return;

        List<ConversationMessage> messages = new ArrayList<>();
        for (ChatMessage cm : chatMessages) {
            if (cm.getType() == ChatMessage.TYPE_USER || cm.getType() == ChatMessage.TYPE_AI) {
                String role = cm.getType() == ChatMessage.TYPE_USER ? "user" : "assistant";
                ConversationMessage msg = new ConversationMessage(role, cm.getMessage());
                messages.add(msg);
            }
        }
        conv.setMessages(messages);
        conv.setThinkingEnabled(thinkingEnabled);
        conv.setSearchEnabled(searchEnabled);

        if (conv.getMessageCount() > 0) {
            String title = chatMessages.get(0).getMessage();
            if (title != null && title.length() > 30) {
                title = title.substring(0, 30) + "...";
            }
            conv.setTitle(title);
        }

        conversationManager.saveConversations();
    }

    private void updateModelDisplay() {
        List<LLMModel> models = modelsManager.getModelsForProvider(selectedProvider);
        String displayName = selectedModelId;
        for (LLMModel m : models) {
            if (m.getId().equals(selectedModelId)) {
                displayName = m.getDisplayName();
                break;
            }
        }
        modelNameText.setText(selectedProvider + " • " + displayName);
    }

    private void addWelcomeMessage() {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_SUGGESTIONS, null, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }

    private void showModelPicker() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.model_picker_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        LinearLayout providersContainer = bottomSheetView.findViewById(R.id.providers_container);
        providersContainer.removeAllViews();

        modelsManager.getModels(new ModelsManager.ModelsFetchCallback() {
            @Override
            public void onSuccess(Map<String, List<LLMModel>> models) {
                runOnUiThread(() -> populateModelPicker(providersContainer, models, bottomSheetDialog));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Map<String, List<LLMModel>> staticModels = new HashMap<>();
                    for (String provider : ProviderFactory.getAvailableProviders()) {
                        staticModels.put(provider, ProviderFactory.getStaticModels(provider));
                    }
                    populateModelPicker(providersContainer, staticModels, bottomSheetDialog);
                });
            }
        });

        bottomSheetDialog.show();
    }

    private void populateModelPicker(LinearLayout container, Map<String, List<LLMModel>> models, BottomSheetDialog dialog) {
        container.removeAllViews();

        for (String provider : ProviderFactory.getAvailableProviders()) {
            TextView providerHeader = new TextView(this);
            providerHeader.setText(provider);
            providerHeader.setTextSize(16);
            providerHeader.setPadding(32, 24, 32, 12);
            providerHeader.setTextColor(getResources().getColor(R.color.md_theme_light_onSurface));
            container.addView(providerHeader);

            LinearLayout providerModelsContainer = new LinearLayout(this);
            providerModelsContainer.setOrientation(LinearLayout.VERTICAL);
            container.addView(providerModelsContainer);

            List<LLMModel> providerModels = models.get(provider);
            if (providerModels == null) {
                providerModels = ProviderFactory.getStaticModels(provider);
            }

            for (LLMModel model : providerModels) {
                TextView modelView = new TextView(this);
                String needsKey = ProviderFactory.requiresApiKey(provider) ? " (Requires Key)" : " (Free)";
                modelView.setText(model.getDisplayName() + needsKey);
                modelView.setTextSize(14);
                modelView.setPadding(48, 16, 48, 16);

                boolean isSelected = provider.equals(selectedProvider) && model.getId().equals(selectedModelId);
                modelView.setTextColor(getResources().getColor(
                    isSelected ? R.color.md_theme_light_primary : R.color.md_theme_light_onSurfaceVariant));

                modelView.setOnClickListener(v -> {
                    if (ProviderFactory.requiresApiKey(provider)) {
                        showApiKeyDialog(provider, model.getId());
                    } else {
                        selectModel(provider, model.getId());
                    }
                    dialog.dismiss();
                });

                providerModelsContainer.addView(modelView);
            }

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0x1F000000);
            container.addView(divider);
        }
    }

    private void showApiKeyDialog(String provider, String modelId) {
        new DialogManager(this, new PreferencesManager(this)).showApiKeyDialog(apiKey -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                selectModel(provider, modelId);
            }
        });
    }

    private void selectModel(String provider, String modelId) {
        selectedProvider = provider;
        selectedModelId = modelId;
        conversationManager.setSelectedProvider(provider);
        conversationManager.setSelectedModel(modelId);
        updateModelDisplay();

        llmProvider = ProviderFactory.create(provider);
    }

    private void sendMessage(String message) {
        if (chatMessages.size() == 1 && chatMessages.get(0).getType() == ChatMessage.TYPE_SUGGESTIONS) {
            chatMessages.remove(0);
            messageAdapter.notifyItemRemoved(0);
        }

        chatMessages.add(new ChatMessage(ChatMessage.TYPE_USER, message, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

        ConversationMessage userMsg = new ConversationMessage("user", message);
        conversationManager.addMessageToCurrentConversation(userMsg);

        generateOutline(message);
    }

    private void generateOutline(String userPrompt) {
        showProgressMessage("Generating outline...", 0);

        if (llmProvider == null) {
            llmProvider = ProviderFactory.create(selectedProvider, getApiKeyIfNeeded());
        }

        if (llmProvider == null) {
            updateProgressMessage("Provider not configured", 0);
            return;
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(createSystemMessage("You are a PDF content planner. Generate detailed outlines for PDF documents."));
        messages.add(createUserMessage(userPrompt));

        Map<String, Object> options = new HashMap<>();
        options.put("thinkingEnabled", thinkingEnabled);
        options.put("searchEnabled", searchEnabled);

        final StringBuilder fullResponse = new StringBuilder();

        llmProvider.generateStream(messages, selectedModelId, options, new LLMProvider.StreamCallback() {
            @Override
            public void onText(String text) {
                fullResponse.append(text);
                runOnUiThread(() -> {
                    updateProgressMessage("Generating outline... " + fullResponse.length() + " chars", 50);
                });
            }

            @Override
            public void onThinking(String thinking) {
            }

            @Override
            public void onToolCall(String id, String name, String arguments) {
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    try {
                        OutlineData outlineData = com.pdf.ai.parser.OutlineParser.parseFromJsonText(fullResponse.toString());
                        removeProgressMessage();
                        currentOutlineData = outlineData;
                        showOutlineInChat(outlineData);
                    } catch (Exception e) {
                        updateProgressMessage("Failed to parse outline: " + e.getMessage(), 0);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> updateProgressMessage("Error: " + error, 0));
            }
        });
    }

    private Map<String, String> createSystemMessage(String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "system");
        msg.put("content", content);
        return msg;
    }

    private Map<String, String> createUserMessage(String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        return msg;
    }

    private String getApiKeyIfNeeded() {
        if (ProviderFactory.requiresApiKey(selectedProvider)) {
            return new PreferencesManager(this).getGeminiApiKey();
        }
        return null;
    }

    private void showProgressMessage(String status, int progress) {
        if (chatMessages.isEmpty() || chatMessages.get(chatMessages.size() - 1).getType() != ChatMessage.TYPE_PROGRESS) {
            chatMessages.add(new ChatMessage(ChatMessage.TYPE_PROGRESS, null, status, progress, null));
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        } else {
            updateProgressMessage(status, progress);
        }
        scrollToBottom();
    }

    private void updateProgressMessage(String status, int progress) {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                lastMessage.setProgressStatus(status);
                lastMessage.setProgressValue(progress);
                messageAdapter.notifyItemChanged(chatMessages.size() - 1);
                scrollToBottom();
            }
        }
    }

    private void removeProgressMessage() {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                int removeIndex = chatMessages.size() - 1;
                chatMessages.remove(removeIndex);
                messageAdapter.notifyItemRemoved(removeIndex);
            }
        }
    }

    private void showOutlineInChat(OutlineData outlineData) {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_OUTLINE, null, null, outlineData));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        saveConversation();
    }

    private void scrollToBottom() {
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        }
    }

    @Override
    public void onApproveOutline(OutlineData outlineData) {
        for (int i = chatMessages.size() - 1; i >= 0; i--) {
            if (chatMessages.get(i).getType() == ChatMessage.TYPE_OUTLINE) {
                chatMessages.remove(i);
                messageAdapter.notifyItemRemoved(i);
                break;
            }
        }
        isGenerating = true;
        executorService.execute(() -> startPdfGeneration(outlineData));
    }

    private void startPdfGeneration(OutlineData approvedOutline) {
        currentPdfSectionsContent = new ArrayList<>();
        runOnUiThread(() -> showProgressMessage("Writing: " + approvedOutline.getSections().get(0) + " (0%)", 0));
        generateSectionContent(approvedOutline, 0);
    }

    private void generateSectionContent(OutlineData outlineData, int sectionIndex) {
        if (sectionIndex >= outlineData.getSections().size()) {
            runOnUiThread(() -> updateProgressMessage("Finalizing PDF...", 100));

            pdfGenerator.createPdf(outlineData.getPdfTitle(), outlineData, currentPdfSectionsContent, new PdfGenerator.PdfGenerationCallback() {
                @Override
                public void onPdfGenerated(String pathOrUri, String pdfTitle) {
                    runOnUiThread(() -> {
                        removeProgressMessage();
                        String cleanedTitle = pdfTitle.replace("A comprehensive", "").trim();
                        Toast.makeText(ChatActivity.this, "PDF created successfully", Toast.LENGTH_LONG).show();
                        chatMessages.add(new ChatMessage(ChatMessage.TYPE_PDF_DOWNLOAD, null, null, null, pathOrUri, cleanedTitle));
                        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
                        scrollToBottom();
                        saveConversation();
                    });
                }

                @Override
                public void onPdfGenerationFailed(String error) {
                    runOnUiThread(() -> {
                        removeProgressMessage();
                        Toast.makeText(ChatActivity.this, "Error creating PDF: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
            isGenerating = false;
            return;
        }

        String sectionTitle = outlineData.getSections().get(sectionIndex);
        int totalSections = outlineData.getSections().size();
        int progress = (int) (((float) sectionIndex / totalSections) * 100);

        runOnUiThread(() -> updateProgressMessage("Writing: " + sectionTitle + " (" + progress + "%)", progress));

        if (llmProvider == null) {
            llmProvider = ProviderFactory.create(selectedProvider, getApiKeyIfNeeded());
            if (llmProvider == null) {
                runOnUiThread(() -> updateProgressMessage("Provider not configured", progress));
                isGenerating = false;
                return;
            }
        }

        String sectionPrompt = buildSectionPrompt(outlineData.getPdfTitle(), sectionTitle, outlineData.getSections(), sectionIndex);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(createSystemMessage("You are a professional PDF content writer. Write detailed, well-structured content in markdown format."));
        messages.add(createUserMessage(sectionPrompt));

        Map<String, Object> options = new HashMap<>();
        options.put("thinkingEnabled", thinkingEnabled);

        final StringBuilder sectionContent = new StringBuilder();

        llmProvider.generateStream(messages, selectedModelId, options, new LLMProvider.StreamCallback() {
            @Override
            public void onText(String text) {
                sectionContent.append(text);
            }

            @Override
            public void onThinking(String thinking) {
            }

            @Override
            public void onToolCall(String id, String name, String arguments) {
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    String cleanedContent = MarkdownParser.normalize(sectionContent.toString());
                    currentPdfSectionsContent.add(cleanedContent);
                    executorService.execute(() -> generateSectionContent(outlineData, sectionIndex + 1));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateProgressMessage("Error generating " + sectionTitle + ": " + error, progress);
                    isGenerating = false;
                });
            }
        });
    }

    private String buildSectionPrompt(String pdfTitle, String sectionTitle, List<String> allSections, int currentIndex) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate detailed and professionally formatted content for a PDF document.\n");
        prompt.append("The overall PDF title is: \"").append(pdfTitle).append("\".\n");
        prompt.append("You are currently writing the section titled: \"").append(sectionTitle).append("\".\n");
        prompt.append("The complete outline of the PDF is:\n");
        for (int i = 0; i < allSections.size(); i++) {
            prompt.append(i + 1).append(". ").append(allSections.get(i));
            if (i == currentIndex) prompt.append(" (Current Section)");
            prompt.append("\n");
        }
        prompt.append("\nProvide the content for the section \"").append(sectionTitle).append("\" only.\n");
        prompt.append("Format the content using standard Markdown. This includes headings (#, ##), lists (* or 1.), bold (**text**), and italic (*text*).\n");
        prompt.append("Do not wrap the response in ```markdown blocks.\n");
        return prompt.toString();
    }

    @Override
    public void onDiscardOutline(int position) {
        if (position != RecyclerView.NO_POSITION) {
            chatMessages.remove(position);
            messageAdapter.notifyItemRemoved(position);
            saveConversation();
        }
    }

    @Override
    public void onSuggestionClick(String prompt) {
        if (!isGenerating) {
            sendMessage(prompt);
        } else {
            Toast.makeText(this, "Please wait until the current PDF generation is complete.", Toast.LENGTH_SHORT).show();
        }
    }
}