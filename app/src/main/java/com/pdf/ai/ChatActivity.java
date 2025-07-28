package com.pdf.ai;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.pdf.ai.GeminiApiClient;
import com.pdf.ai.PdfGenerator;
import com.pdf.ai.DialogManager;
import com.pdf.ai.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity implements
        MessageAdapter.OnOutlineActionListener,
        MessageAdapter.OnSuggestionClickListener {

    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessages;
    private EditText messageEditText;
    private LinearLayout sendButton;
    private ImageView settingsIcon;
    private TextView modelNameText;

    private String geminiApiKey;
    private String selectedModel = "gemini-1.5-flash-latest"; // Default model

    private PreferencesManager preferencesManager;
    private GeminiApiClient geminiApiClient;
    private PdfGenerator pdfGenerator;
    private DialogManager dialogManager;

    private OutlineData currentOutlineData;
    private boolean isGeneratingPdf = false;
    private List<String> currentPdfSectionsContent;

    // Executor Service for background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        preferencesManager = new PreferencesManager(this);
        geminiApiClient = new GeminiApiClient();
        pdfGenerator = new PdfGenerator(this);
        dialogManager = new DialogManager(this, preferencesManager);

        geminiApiKey = preferencesManager.getGeminiApiKey();
        selectedModel = preferencesManager.getSelectedModel();

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        settingsIcon = findViewById(R.id.icon_settings_round);
        modelNameText = findViewById(R.id.model_name);
        LinearLayout modelPickerLayout = findViewById(R.id.model_picker);

        modelNameText.setText(selectedModel);

        chatMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, chatMessages, this, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        loadChatHistory();
        if (chatMessages.isEmpty()) {
            addWelcomeMessage();
        }

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendUserMessage(message);
                messageEditText.setText("");
            }
        });

        settingsIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        modelPickerLayout.setOnClickListener(v -> showModelPicker());

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

    private void addWelcomeMessage() {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_SUGGESTIONS, null, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void showApiKeyDialog() {
        dialogManager.showApiKeyDialog(apiKey -> {
            geminiApiKey = apiKey;
        });
    }

    private void showModelPicker() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.model_picker_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        LinearLayout modelList = bottomSheetView.findViewById(R.id.model_list);
        String[] models = {
                "gemini-1.5-flash-latest",
                "gemini-1.5-pro-latest",
                "gemini-2.5-flash-lite-preview-06-17",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite"
        };

        for (String model : models) {
            TextView textView = new TextView(this);
            textView.setText(model);
            textView.setTextSize(14);
            textView.setPadding(40, 20, 40, 20);
            textView.setOnClickListener(v -> {
                selectedModel = model;
                modelNameText.setText(selectedModel);
                preferencesManager.setSelectedModel(selectedModel);
                bottomSheetDialog.dismiss();
            });
            modelList.addView(textView);
        }

        bottomSheetDialog.show();
    }

    private void sendUserMessage(String message) {
        if (chatMessages.size() == 1 && chatMessages.get(0).getType() == ChatMessage.TYPE_SUGGESTIONS) {
            chatMessages.remove(0);
            messageAdapter.notifyItemRemoved(0);
        }

        chatMessages.add(new ChatMessage(ChatMessage.TYPE_USER, message, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        startOutlineGeneration(message);
    }

    private void showProgressMessage(String status, int progressValue) {
        if (chatMessages.isEmpty() || chatMessages.get(chatMessages.size() - 1).getType() != ChatMessage.TYPE_PROGRESS) {
            chatMessages.add(new ChatMessage(ChatMessage.TYPE_PROGRESS, null, status, progressValue, null));
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        } else {
            updateProgressMessage(status, progressValue);
        }
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
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
        saveChatHistory();
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
                messageAdapter.notifyItemRemoved(chatMessages.size() - 1);
            }
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
        isGeneratingPdf = true;
        executorService.execute(() -> startPdfGeneration(outlineData));
    }

    private void startPdfGeneration(OutlineData approvedOutline) {
        currentPdfSectionsContent = new ArrayList<>();
        runOnUiThread(() -> showProgressMessage("Writing content for: " + approvedOutline.getSections().get(0) + " (0%)", 0));
        generateSectionContent(approvedOutline, 0);
    }

    private void generateSectionContent(OutlineData outlineData, int sectionIndex) {
        if (sectionIndex >= outlineData.getSections().size()) {
            runOnUiThread(() -> {
                updateProgressMessage("All content generated. Finalizing PDF...", 100);
            });

            pdfGenerator.createPdf(outlineData.getPdfTitle(), outlineData, currentPdfSectionsContent, new PdfGenerator.PdfGenerationCallback() {
                @Override
                public void onPdfGenerated(String pathOrUri, String pdfTitle) {
                    runOnUiThread(() -> {
                        removeProgressMessage();
                        String cleanedTitle = pdfTitle.replace("A comprehensive", "").trim();
                        Toast.makeText(ChatActivity.this, "PDF created successfully", Toast.LENGTH_LONG).show();
                        chatMessages.add(new ChatMessage(ChatMessage.TYPE_PDF_DOWNLOAD, null, null, null, pathOrUri, cleanedTitle));
                        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                        saveChatHistory();
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
            isGeneratingPdf = false;
            return;
        }

        String sectionTitle = outlineData.getSections().get(sectionIndex);
        int totalSections = outlineData.getSections().size();
        int progress = (int) (((float) sectionIndex / totalSections) * 100);

        runOnUiThread(() -> updateProgressMessage("Writing content for: " + sectionTitle + " (" + progress + "%)", progress));

        geminiApiClient.generateSectionContent(outlineData.getPdfTitle(), sectionTitle, outlineData.getSections(), sectionIndex, geminiApiKey, selectedModel, new GeminiApiClient.GeminiApiCallback() {
            @Override
            public void onSuccess(String response) {
                
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String sectionContent = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        
                        // FIX: Clean the markdown content before adding it to the list
                        String cleanedContent = cleanMarkdown(sectionContent);
                        currentPdfSectionsContent.add(cleanedContent);
                        
                        executorService.execute(() -> generateSectionContent(outlineData, sectionIndex + 1));
                    } catch (JSONException e) {
                        updateProgressMessage("Failed to parse Gemini response for section content: " + e.getMessage(), progress);
                        isGeneratingPdf = false;
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                
                runOnUiThread(() -> {
                    updateProgressMessage("Error generating content for " + sectionTitle + ": " + error, progress);
                    isGeneratingPdf = false;
                });
            }
        });
    }

    private void saveChatHistory() {
        preferencesManager.saveChatHistory(chatMessages);
    }

    private void loadChatHistory() {
        chatMessages.addAll(preferencesManager.loadChatHistory());
        messageAdapter.notifyDataSetChanged();
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    @Override
    public void onDiscardOutline(int position) {
        if (position != RecyclerView.NO_POSITION) {
            chatMessages.remove(position);
            messageAdapter.notifyItemRemoved(position);
            saveChatHistory();
        }
    }

    private String cleanJson(String jsonString) {
        if (jsonString.startsWith("```json")) {
            jsonString = jsonString.substring(7);
        }
        if (jsonString.endsWith("```")) {
            jsonString = jsonString.substring(0, jsonString.length() - 3);
        }
        return jsonString.trim();
    }

    // FIX: Added a new method to clean markdown content specifically.
    private String cleanMarkdown(String markdownString) {
        if (markdownString == null) return "";
        
        // Remove markdown code block delimiters (e.g., ```markdown ... ```)
        if (markdownString.trim().startsWith("```markdown")) {
            markdownString = markdownString.trim().substring(11);
        } else if (markdownString.trim().startsWith("```")) {
            markdownString = markdownString.trim().substring(3);
        }
        
        if (markdownString.trim().endsWith("```")) {
            markdownString = markdownString.trim().substring(0, markdownString.trim().length() - 3);
        }
        
        return markdownString.trim();
    }

    @Override
    public void onSuggestionClick(String prompt) {
        if (!isGeneratingPdf) {
            sendUserMessage(prompt);
        } else {
            Toast.makeText(this, "Please wait until the current PDF generation is complete.", Toast.LENGTH_SHORT).show();
        }
    }
}
