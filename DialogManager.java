package com.pdf.ai;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import com.pdf.ai.R;

public class DialogManager {
	
	private Context context;
	private PreferencesManager preferencesManager;
	
	public interface ApiKeyDialogListener {
		void onApiKeySet(String apiKey);
	}
	
	public interface SettingsDialogListener {
		void onSettingsSaved(String apiKey, String selectedModel);
	}
	
	public DialogManager(Context context, PreferencesManager preferencesManager) {
		this.context = context;
		this.preferencesManager = preferencesManager;
	}
	
	public void showApiKeyDialog(ApiKeyDialogListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Enter Gemini API Key");

        final TextInputLayout textInputLayout = new TextInputLayout(context);
        textInputLayout.setHint("Your Gemini API Key");
        textInputLayout.setPadding(20, 20, 20, 20);
        final TextInputEditText input = new TextInputEditText(context);
        textInputLayout.addView(input);

        builder.setView(textInputLayout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String apiKey = input.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                preferencesManager.setGeminiApiKey(apiKey);
                Toast.makeText(context, "API Key saved!", Toast.LENGTH_SHORT).show();
                listener.onApiKeySet(apiKey);
            } else {
                Toast.makeText(context, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void showSettingsDialog(String currentApiKey, String currentSelectedModel, SettingsDialogListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Settings");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView apiKeyLabel = new TextView(context);
        apiKeyLabel.setText("Gemini API Key:");
        apiKeyLabel.setTextAppearance(R.style.TextAppearance_Material3_LabelMedium);
        layout.addView(apiKeyLabel);

        final TextInputLayout apiKeyLayout = new TextInputLayout(context);
        apiKeyLayout.setHint("Enter API Key");
        final TextInputEditText apiKeyInput = new TextInputEditText(context);
        apiKeyInput.setText(currentApiKey);
        apiKeyLayout.addView(apiKeyInput);
        layout.addView(apiKeyLayout);

        TextView modelLabel = new TextView(context);
        modelLabel.setText("Select Model:");
        modelLabel.setTextAppearance(R.style.TextAppearance_Material3_LabelMedium);
        layout.addView(modelLabel);

        String[] models = {
            "gemini-2.5-flash-lite-preview-06-17",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro-latest"
        };
        int checkedItem = 0;
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(currentSelectedModel)) {
                checkedItem = i;
                break;
            }
        }

        final String[] selectedModel = {currentSelectedModel};
        builder.setSingleChoiceItems(models, checkedItem, (dialog, which) -> {
            selectedModel[0] = models[which];
        });

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newApiKey = apiKeyInput.getText().toString().trim();
            if (!newApiKey.isEmpty()) {
                preferencesManager.setGeminiApiKey(newApiKey);
                Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show();
                listener.onSettingsSaved(newApiKey, selectedModel[0]);
            } else {
                Toast.makeText(context, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
