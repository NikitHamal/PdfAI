package com.pdf.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelSelectorComponent {

    public interface OnModelSelectedListener {
        void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo);
    }

    private Context context;
    private OnModelSelectedListener listener;
    private String selectedModelId;
    private Map<String, QwenApiClient.ModelInfo> availableModels;
    private AlertDialog dialog;

    public ModelSelectorComponent(Context context, OnModelSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.availableModels = QwenApiClient.getAvailableModels();
    }

    public View createSelectorView() {
        View selectorView = LayoutInflater.from(context).inflate(R.layout.model_selector_view, null);
        TextView modelNameText = selectorView.findViewById(R.id.tv_model_name);
        ImageView dropdownArrow = selectorView.findViewById(R.id.iv_dropdown_arrow);

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

    private void showModelSelectionDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_model_selector, null);
        RecyclerView modelsRecyclerView = dialogView.findViewById(R.id.models_recycler_view);
        modelsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        List<Map.Entry<String, QwenApiClient.ModelInfo>> modelList = new ArrayList<>(availableModels.entrySet());
        ModelAdapter adapter = new ModelAdapter(modelList, model -> {
            selectedModelId = model.getKey();
            if (listener != null) {
                listener.onModelSelected(model.getKey(), model.getValue());
            }
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });
        modelsRecyclerView.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.show();
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