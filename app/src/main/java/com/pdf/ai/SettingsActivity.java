package com.pdf.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity implements ModelSelectorComponent.OnModelSelectedListener {

    private PreferencesManager preferencesManager;
    private TextInputEditText apiKeyEditText;
    private MaterialSwitch themeSwitch;
    private FrameLayout modelSelectorContainer;
    private ModelSelectorComponent modelSelectorComponent;

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
        modelSelectorContainer = findViewById(R.id.fl_settings_model_selector_container);
        themeSwitch = findViewById(R.id.theme_switch);

        // Load saved API key
        apiKeyEditText.setText(preferencesManager.getGeminiApiKey());

        // Setup model selector
        setupModelSelector();

        saveApiKeyButton.setOnClickListener(v -> {
            String apiKey = apiKeyEditText.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                preferencesManager.setGeminiApiKey(apiKey);
                Toast.makeText(this, "API Key saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case android.content.res.Configuration.UI_MODE_NIGHT_NO:
                themeSwitch.setChecked(false);
                break;
            case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                themeSwitch.setChecked(true);
                break;
        }

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupModelSelector() {
        modelSelectorComponent = new ModelSelectorComponent(this, this);
        modelSelectorComponent.setSelectedModel(preferencesManager.getSelectedModel());
        View selectorView = modelSelectorComponent.createSelectorView();
        modelSelectorContainer.addView(selectorView);
    }

    @Override
    public void onModelSelected(String modelId, QwenApiClient.ModelInfo modelInfo) {
        preferencesManager.setSelectedModel(modelId);
        Toast.makeText(this, "Default model updated to " + modelInfo.displayName, Toast.LENGTH_SHORT).show();
        
        // Update the selector view
        View selectorView = modelSelectorContainer.getChildAt(0);
        if (selectorView != null) {
            modelSelectorComponent.updateSelectorView(selectorView, modelId);
        }
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
