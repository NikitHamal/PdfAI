package com.pdf.ai;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private TextInputEditText apiKeyEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        preferencesManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        apiKeyEditText = findViewById(R.id.api_key_edit_text);
        MaterialButton saveApiKeyButton = findViewById(R.id.save_api_key_button);

        String savedKey = preferencesManager.getGeminiApiKey();
        if (savedKey != null && !savedKey.isEmpty()) {
            apiKeyEditText.setText(savedKey);
        }

        saveApiKeyButton.setOnClickListener(v -> {
            String apiKey = apiKeyEditText.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                preferencesManager.setGeminiApiKey(apiKey);
                Toast.makeText(this, "API Key saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            }
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