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

    
}
