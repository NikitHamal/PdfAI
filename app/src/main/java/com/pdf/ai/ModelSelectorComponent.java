package com.pdf.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class ModelSelectorComponent {

    public interface OnModelSelectedListener {
        void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo);
    }

    private Context context;
    private OnModelSelectedListener listener;
    private String selectedModelId;
    private Map<String, QwenApiClient.ModelInfo> availableModels;

    public ModelSelectorComponent(Context context, OnModelSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.availableModels = QwenApiClient.getAvailableModels();
    }

    public View createSelectorView() {
        View selectorView = LayoutInflater.from(context).inflate(R.layout.model_selector_view, null);
        TextView modelNameText = selectorView.findViewById(R.id.tv_model_name);
        ImageView dropdownArrow = selectorView.findViewById(R.id.iv_dropdown_arrow);

        // Set initial model if selected
        if (selectedModelId != null && availableModels.containsKey(selectedModelId)) {
            QwenApiClient.ModelInfo modelInfo = availableModels.get(selectedModelId);
            modelNameText.setText(modelInfo.displayName);
        }

        selectorView.setOnClickListener(v -> showModelSelectionDialog());

        return selectorView;
    }

    public void setSelectedModel(String modelId) {
        this.selectedModelId = modelId;
    }

    public String getSelectedModel() {
        return selectedModelId;
    }

    private void showModelSelectionDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_model_selector, null);
        LinearLayout modelsContainer = dialogView.findViewById(R.id.ll_models_container);

        // Add Qwen models section
        boolean qwenSectionAdded = false;
        for (Map.Entry<String, QwenApiClient.ModelInfo> entry : availableModels.entrySet()) {
            String modelId = entry.getKey();
            QwenApiClient.ModelInfo modelInfo = entry.getValue();

            if (modelInfo.isQwenModel && !qwenSectionAdded) {
                qwenSectionAdded = true;
            } else if (modelInfo.isQwenModel) {
                // Add Qwen model
                View modelItemView = createModelItemView(modelId, modelInfo);
                modelsContainer.addView(modelItemView);
            }
        }

        // Add Gemini models section
        for (Map.Entry<String, QwenApiClient.ModelInfo> entry : availableModels.entrySet()) {
            String modelId = entry.getKey();
            QwenApiClient.ModelInfo modelInfo = entry.getValue();

            if (!modelInfo.isQwenModel) {
                View modelItemView = createModelItemView(modelId, modelInfo);
                modelsContainer.addView(modelItemView);
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(dialogView);
        builder.show();
    }

    private View createModelItemView(String modelId, QwenApiClient.ModelInfo modelInfo) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_model_selector, null);
        
        TextView displayNameText = itemView.findViewById(R.id.tv_model_display_name);
        TextView descriptionText = itemView.findViewById(R.id.tv_model_description);
        TextView thinkingBadge = itemView.findViewById(R.id.tv_thinking_badge);
        TextView websearchBadge = itemView.findViewById(R.id.tv_websearch_badge);
        ImageView selectedIndicator = itemView.findViewById(R.id.iv_selected_indicator);

        displayNameText.setText(modelInfo.displayName);
        descriptionText.setText(modelInfo.description);

        // Show feature badges
        if (modelInfo.supportsThinking) {
            thinkingBadge.setVisibility(View.VISIBLE);
        }
        if (modelInfo.supportsWebSearch) {
            websearchBadge.setVisibility(View.VISIBLE);
        }

        // Show selected indicator
        if (modelId.equals(selectedModelId)) {
            selectedIndicator.setVisibility(View.VISIBLE);
        }

        itemView.setOnClickListener(v -> {
            selectedModelId = modelId;
            if (listener != null) {
                listener.onModelSelected(modelId, modelInfo);
            }
        });

        return itemView;
    }

    public void updateSelectorView(View selectorView, String modelId) {
        TextView modelNameText = selectorView.findViewById(R.id.tv_model_name);
        if (availableModels.containsKey(modelId)) {
            QwenApiClient.ModelInfo modelInfo = availableModels.get(modelId);
            modelNameText.setText(modelInfo.displayName);
            this.selectedModelId = modelId;
        }
    }
}