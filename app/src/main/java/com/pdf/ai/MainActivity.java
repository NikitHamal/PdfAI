package com.pdf.ai;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.LinearLayout;
import com.pdf.ai.ChatMessage;
import com.pdf.ai.OutlineData;
import com.pdf.ai.PdfGenerator;
import com.pdf.ai.DialogManager;
import com.pdf.ai.PreferencesManager;

 
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
 

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.TextView;
import com.pdf.ai.provider.LLMProvider;
import com.pdf.ai.provider.ProviderFactory;
import com.pdf.ai.util.MarkdownParser;
import com.pdf.ai.ui.interaction.OnOutlineActionListener;
import com.pdf.ai.ui.interaction.OnSuggestionClickListener;

public class MainActivity extends AppCompatActivity implements
        OnOutlineActionListener,
        OnSuggestionClickListener,
        ChatManager.OnUserMessageSentListener {

    private EditText messageEditText;
    private LinearLayout sendButton;
    private ImageView settingsIcon;
    private TextView modelNameText;

    private String geminiApiKey;
    private String selectedModel = "gemini-2.5-flash"; // Default model (Gemini 2.x)
    private String selectedProvider = "Gemini"; // "Gemini" or "GPT-OSS"

    private PreferencesManager preferencesManager;
    private LLMProvider llmProvider;
    private PdfGenerator pdfGenerator;
    private DialogManager dialogManager;
    private ChatManager chatManager;
    private PdfGenerationManager pdfGenerationManager;

    private OutlineData currentOutlineData;
    private boolean isGeneratingPdf = false;

    // Executor Service for background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        preferencesManager = new PreferencesManager(this);
        pdfGenerator = new PdfGenerator(this);
        dialogManager = new DialogManager(this, preferencesManager);

        geminiApiKey = preferencesManager.getGeminiApiKey();
        selectedModel = preferencesManager.getSelectedModel();
        selectedProvider = preferencesManager.getSelectedProvider();
        llmProvider = ProviderFactory.create(selectedProvider, selectedModel, geminiApiKey);

        RecyclerView chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        settingsIcon = findViewById(R.id.settings_icon);
        modelNameText = findViewById(R.id.model_name_text);
        LinearLayout modelPickerLayout = findViewById(R.id.model_picker_layout);

        modelNameText.setText(selectedProvider + " • " + selectedModel);

        List<ChatMessage> chatMessages = new ArrayList<>();
        MessageAdapter messageAdapter = new MessageAdapter(this, chatMessages, this, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        chatManager = new ChatManager(this, chatRecyclerView, chatMessages, messageAdapter, preferencesManager, this);
        pdfGenerationManager = new PdfGenerationManager(this, llmProvider, pdfGenerator, chatManager);

        chatManager.loadChatHistory();
        if (chatMessages.isEmpty()) {
            chatManager.addWelcomeMessage();
        }

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                chatManager.sendUserMessage(message);
                messageEditText.setText("");
            }
        });

        settingsIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        modelPickerLayout.setOnClickListener(v -> showModelPicker());

        if ("Gemini".equals(selectedProvider)) {
            if (geminiApiKey == null || geminiApiKey.isEmpty()) {
                showApiKeyDialog();
            }
        }

        // Initialize the ExecutorService
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service when the activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }


    private void showApiKeyDialog() {
        dialogManager.showApiKeyDialog(apiKey -> {
            geminiApiKey = apiKey;
            // Recreate provider in case user set API key after prompt
            llmProvider = ProviderFactory.create(selectedProvider, selectedModel, geminiApiKey);
        });
    }

    private void showModelPicker() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.model_picker_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        LinearLayout geminiList = bottomSheetView.findViewById(R.id.gemini_list);
        LinearLayout gptossList = bottomSheetView.findViewById(R.id.gptoss_list);

        String[] geminiModels = new String[] {
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite"
        };

        for (String model : geminiModels) {
            TextView tv = new TextView(this);
            tv.setText("Gemini • " + model);
            tv.setTextSize(14);
            tv.setPadding(40, 20, 40, 20);
            tv.setOnClickListener(v -> {
                selectedProvider = "Gemini";
                selectedModel = model;
                preferencesManager.setSelectedProvider(selectedProvider);
                preferencesManager.setSelectedModel(selectedModel);
                modelNameText.setText(selectedProvider + " • " + selectedModel);
                llmProvider = ProviderFactory.create(selectedProvider, selectedModel, geminiApiKey);
                bottomSheetDialog.dismiss();
            });
            geminiList.addView(tv);
        }

        String[] gptOssModels = new String[] {
                "gpt-oss-120b",
                "gpt-oss-20b"
        };
        for (String model : gptOssModels) {
            TextView tv = new TextView(this);
            tv.setText("GPT-OSS • " + model + " (Free)");
            tv.setTextSize(14);
            tv.setPadding(40, 20, 40, 20);
            tv.setOnClickListener(v -> {
                selectedProvider = "GPT-OSS";
                selectedModel = model;
                preferencesManager.setSelectedProvider(selectedProvider);
                preferencesManager.setSelectedModel(selectedModel);
                modelNameText.setText(selectedProvider + " • " + selectedModel);
                llmProvider = ProviderFactory.create(selectedProvider, selectedModel, geminiApiKey);
                bottomSheetDialog.dismiss();
            });
            gptossList.addView(tv);
        }

        bottomSheetDialog.show();
    }



    @Override
    public void onUserMessageSent(String message) {
        startOutlineGeneration(message);
    }

    private void startOutlineGeneration(String userPrompt) {
        chatManager.showProgressMessage("Sending request...", 0);
        if ("Gemini".equals(selectedProvider) && (geminiApiKey == null || geminiApiKey.isEmpty())) {
            Toast.makeText(this, "Gemini API Key is not set. Please set it in settings.", Toast.LENGTH_LONG).show();
            chatManager.updateProgressMessage("Error: API Key missing.", 0);
            return;
        }
        if (llmProvider == null) {
            llmProvider = ProviderFactory.create(selectedProvider, selectedModel, geminiApiKey);
        }
        if (llmProvider == null) {
            chatManager.updateProgressMessage("Provider not configured.", 0);
            return;
        }
        llmProvider.generateOutline(userPrompt, new LLMProvider.OutlineCallback() {
            @Override
            public void onSuccess(OutlineData outlineData) {
                runOnUiThread(() -> {
                    currentOutlineData = outlineData;
                    chatManager.removeProgressMessage();
                    chatManager.showOutlineInChat(outlineData);
                    chatManager.saveChatHistory();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> chatManager.updateProgressMessage("Failed to generate outline: " + error, 0));
            }
        });
    }

    @Override
    public void onApproveOutline(OutlineData outlineData) {
        chatManager.removeOutlineFromChat();
        isGeneratingPdf = true;
        executorService.execute(() -> pdfGenerationManager.startPdfGeneration(outlineData));
    }

    @Override
    public void onDiscardOutline(int position) {
        // This is now handled by the MessageAdapter and ChatManager
    }

    @Override
    public void onSuggestionClick(String prompt) {
        if (!isGeneratingPdf) {
            chatManager.sendUserMessage(prompt);
        } else {
            Toast.makeText(this, "Please wait until the current PDF generation is complete.", Toast.LENGTH_SHORT).show();
        }
    }
}
