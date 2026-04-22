package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pdf.ai.adapter.ConversationAdapter;
import com.pdf.ai.manager.ConversationManager;
import com.pdf.ai.manager.ModelsManager;
import com.pdf.ai.model.Conversation;
import com.pdf.ai.model.LLMModel;
import com.pdf.ai.provider.LLMProvider;
import com.pdf.ai.provider.ProviderFactory;

import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements ConversationAdapter.OnConversationClickListener {
    
    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter adapter;
    private ConversationManager conversationManager;
    private ModelsManager modelsManager;
    private LinearLayout emptyState;
    private TextView selectedModelText;
    
    private String selectedProvider;
    private String selectedModelId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        conversationManager = new ConversationManager(this);
        modelsManager = ModelsManager.getInstance(this);
        
        selectedProvider = conversationManager.getSelectedProvider();
        selectedModelId = conversationManager.getSelectedModel();
        
        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        selectedModelText = findViewById(R.id.selected_model_text);
        FloatingActionButton newChatFab = findViewById(R.id.new_chat_fab);
        LinearLayout modelPickerLayout = findViewById(R.id.model_picker_layout);
        ImageView settingsIcon = findViewById(R.id.settings_icon);
        
        adapter = new ConversationAdapter(this, conversationManager.getConversations(), this);
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversationsRecyclerView.setAdapter(adapter);
        
        updateModelDisplay();
        updateEmptyState();
        
        newChatFab.setOnClickListener(v -> createNewConversation());
        
        modelPickerLayout.setOnClickListener(v -> showModelPicker());
        
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshConversations();
    }
    
    private void refreshConversations() {
        conversationManager.loadConversations();
        adapter.updateData(conversationManager.getConversations());
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (conversationManager.getConversations().isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            conversationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
        }
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
        selectedModelText.setText(selectedProvider + " • " + displayName);
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
                runOnUiThread(() -> {
                    providersContainer.removeAllViews();
                    
                    for (String provider : ProviderFactory.getAvailableProviders()) {
                        TextView providerHeader = new TextView(HomeActivity.this);
                        providerHeader.setText(provider);
                        providerHeader.setTextSize(16);
                        providerHeader.setPadding(32, 24, 32, 12);
                        providerHeader.setTextColor(getResources().getColor(R.color.md_theme_light_onSurface));
                        providersContainer.addView(providerHeader);
                        
                        LinearLayout providerModelsContainer = new LinearLayout(HomeActivity.this);
                        providerModelsContainer.setOrientation(LinearLayout.VERTICAL);
                        providersContainer.addView(providerModelsContainer);
                        
                        List<LLMModel> providerModels = models.get(provider);
                        if (providerModels == null) {
                            providerModels = ProviderFactory.getStaticModels(provider);
                        }
                        
                        for (LLMModel model : providerModels) {
                            TextView modelView = new TextView(HomeActivity.this);
                            String needsKey = ProviderFactory.requiresApiKey(provider) ? " (Requires Key)" : " (Free)";
                            modelView.setText(model.getDisplayName() + needsKey);
                            modelView.setTextSize(14);
                            modelView.setPadding(48, 16, 48, 16);
                            modelView.setTextColor(getResources().getColor(R.color.md_theme_light_onSurfaceVariant));
                            
                            boolean isSelected = provider.equals(selectedProvider) && model.getId().equals(selectedModelId);
                            if (isSelected) {
                                modelView.setTextColor(getResources().getColor(R.color.md_theme_light_primary));
                            }
                            
                            modelView.setOnClickListener(v -> {
                                if (ProviderFactory.requiresApiKey(provider)) {
                                    showApiKeyDialog(provider, model);
                                } else {
                                    selectModel(provider, model.getId());
                                }
                                bottomSheetDialog.dismiss();
                            });
                            
                            providerModelsContainer.addView(modelView);
                        }
                        
                        View divider = new View(HomeActivity.this);
                        divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        divider.setBackgroundColor(0x1F000000);
                        providersContainer.addView(divider);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    for (String provider : ProviderFactory.getAvailableProviders()) {
                        TextView providerHeader = new TextView(HomeActivity.this);
                        providerHeader.setText(provider);
                        providerHeader.setTextSize(16);
                        providerHeader.setPadding(32, 24, 32, 12);
                        providersContainer.addView(providerHeader);
                        
                        List<LLMModel> providerModels = ProviderFactory.getStaticModels(provider);
                        for (LLMModel model : providerModels) {
                            TextView modelView = new TextView(HomeActivity.this);
                            modelView.setText(model.getDisplayName());
                            modelView.setTextSize(14);
                            modelView.setPadding(48, 16, 48, 16);
                            modelView.setOnClickListener(v -> {
                                selectModel(provider, model.getId());
                                bottomSheetDialog.dismiss();
                            });
                            providersContainer.addView(modelView);
                        }
                    }
                });
            }
        });
        
        bottomSheetDialog.show();
    }
    
    private void showApiKeyDialog(String provider, LLMModel model) {
        new DialogManager(this, new PreferencesManager(this)).showApiKeyDialog(apiKey -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                selectModel(provider, model.getId());
            }
        });
    }
    
    private void selectModel(String provider, String modelId) {
        selectedProvider = provider;
        selectedModelId = modelId;
        conversationManager.setSelectedProvider(provider);
        conversationManager.setSelectedModel(modelId);
        updateModelDisplay();
    }
    
    private void createNewConversation() {
        Conversation conv = conversationManager.createConversation(selectedProvider, selectedModelId);
        openConversation(conv.getId());
    }
    
    @Override
    public void onConversationClick(String conversationId) {
        openConversation(conversationId);
    }
    
    @Override
    public void onConversationDelete(String conversationId) {
        conversationManager.deleteConversation(conversationId);
        refreshConversations();
    }
    
    private void openConversation(String conversationId) {
        conversationManager.setCurrentConversationById(conversationId);
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversation_id", conversationId);
        startActivity(intent);
    }
}