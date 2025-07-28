package com.pdf.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SettingsActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private TextInputEditText apiKeyEditText;
    private AutoCompleteTextView modelSpinnerAutocomplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        preferencesManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        apiKeyEditText = findViewById(R.id.api_key_edit_text);
        MaterialButton saveApiKeyButton = findViewById(R.id.save_api_key_button);
        modelSpinnerAutocomplete = findViewById(R.id.model_spinner_autocomplete);

        // Load saved API key and model
        apiKeyEditText.setText(preferencesManager.getGeminiApiKey());

        // Set up model spinner
        String[] models = {
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro-latest",
            "gemini-2.5-flash-lite-preview-06-17",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, models);
        modelSpinnerAutocomplete.setAdapter(adapter);
        modelSpinnerAutocomplete.setText(preferencesManager.getSelectedModel(), false);

        saveApiKeyButton.setOnClickListener(v -> {
            String apiKey = apiKeyEditText.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                preferencesManager.setGeminiApiKey(apiKey);
                Toast.makeText(this, "API Key saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        modelSpinnerAutocomplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedModel = (String) parent.getItemAtPosition(position);
            preferencesManager.setSelectedModel(selectedModel);
            Toast.makeText(this, "Default model saved!", Toast.LENGTH_SHORT).show();
        });
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
