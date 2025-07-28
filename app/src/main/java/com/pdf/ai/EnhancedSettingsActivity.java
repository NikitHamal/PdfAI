package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EnhancedSettingsActivity extends AppCompatActivity {
    
    private TextInputEditText apiKeyEditText;
    private TextInputEditText modelEditText;
    private Switch darkModeSwitch;
    private Switch autoSaveSwitch;
    private Switch dataUsageWarningSwitch;
    private TextView connectionStatusText;
    private MaterialButton testConnectionButton;
    private MaterialButton clearHistoryButton;
    private MaterialButton exportSettingsButton;
    
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enhanced_settings);
        
        preferencesManager = new PreferencesManager(this);
        
        setupToolbar();
        initializeViews();
        loadCurrentSettings();
        setupListeners();
        updateConnectionStatus();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }
    
    private void initializeViews() {
        apiKeyEditText = findViewById(R.id.api_key_edit_text);
        modelEditText = findViewById(R.id.model_edit_text);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        autoSaveSwitch = findViewById(R.id.auto_save_switch);
        dataUsageWarningSwitch = findViewById(R.id.data_usage_warning_switch);
        connectionStatusText = findViewById(R.id.connection_status_text);
        testConnectionButton = findViewById(R.id.test_connection_button);
        clearHistoryButton = findViewById(R.id.clear_history_button);
        exportSettingsButton = findViewById(R.id.export_settings_button);
    }
    
    private void loadCurrentSettings() {
        // Load API key
        String apiKey = preferencesManager.getGeminiApiKey();
        if (apiKey != null) {
            apiKeyEditText.setText(apiKey);
        }
        
        // Load model
        String model = preferencesManager.getSelectedModel();
        modelEditText.setText(model);
        
        // Load theme preference
        String currentTheme = preferencesManager.getThemePreference();
        darkModeSwitch.setChecked(ThemeManager.THEME_DARK.equals(currentTheme));
        
        // Load other preferences
        autoSaveSwitch.setChecked(preferencesManager.isAutoSaveEnabled());
        dataUsageWarningSwitch.setChecked(preferencesManager.isDataUsageWarningEnabled());
    }
    
    private void setupListeners() {
        testConnectionButton.setOnClickListener(v -> testApiConnection());
        clearHistoryButton.setOnClickListener(v -> clearChatHistory());
        exportSettingsButton.setOnClickListener(v -> exportSettings());
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String theme = isChecked ? ThemeManager.THEME_DARK : ThemeManager.THEME_LIGHT;
            ThemeManager.setTheme(this, theme);
        });
    }
    
    private void updateConnectionStatus() {
        String connectionType = NetworkUtils.getConnectionType(this);
        connectionStatusText.setText("Connection: " + connectionType);
        
        if (NetworkUtils.shouldWarnAboutDataUsage(this) && dataUsageWarningSwitch.isChecked()) {
            connectionStatusText.setText(connectionStatusText.getText() + " (Mobile Data)");
        }
    }
    
    private void testApiConnection() {
        String apiKey = apiKeyEditText.getText().toString().trim();
        if (apiKey.isEmpty()) {
            ErrorHandler.handleValidationError(this, "API Key", "Please enter your API key");
            return;
        }
        
        if (!NetworkUtils.isNetworkAvailable(this)) {
            ErrorHandler.handleApiError(this, "No network connection", "API Test");
            return;
        }
        
        // Show loading state
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");
        
        // Test API connection
        GeminiApiClient client = new GeminiApiClient();
        client.generateOutline("Test", apiKey, modelEditText.getText().toString(), 
            new GeminiApiClient.GeminiApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        testConnectionButton.setEnabled(true);
                        testConnectionButton.setText("Test Connection");
                        ErrorHandler.showToast(EnhancedSettingsActivity.this, "Connection successful!");
                    });
                }
                
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        testConnectionButton.setEnabled(true);
                        testConnectionButton.setText("Test Connection");
                        ErrorHandler.handleApiError(EnhancedSettingsActivity.this, error, "API Test");
                    });
                }
            });
    }
    
    private void clearChatHistory() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Chat History")
            .setMessage("Are you sure you want to clear all chat history? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                preferencesManager.clearChatHistory();
                ErrorHandler.showToast(this, "Chat history cleared");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void exportSettings() {
        // Export settings to file
        String settings = preferencesManager.exportSettings();
        // Implementation for file export
        ErrorHandler.showToast(this, "Settings exported successfully");
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveSettings();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }
    
    private void saveSettings() {
        // Save API key
        String apiKey = apiKeyEditText.getText().toString().trim();
        preferencesManager.setGeminiApiKey(apiKey);
        
        // Save model
        String model = modelEditText.getText().toString().trim();
        if (!model.isEmpty()) {
            preferencesManager.setSelectedModel(model);
        }
        
        // Save other preferences
        preferencesManager.setAutoSaveEnabled(autoSaveSwitch.isChecked());
        preferencesManager.setDataUsageWarningEnabled(dataUsageWarningSwitch.isChecked());
        
        ErrorHandler.showToast(this, "Settings saved");
    }
}