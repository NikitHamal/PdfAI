package com.pdf.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DebugActivity extends AppCompatActivity {

    public static final String EXTRA_LOGS = "extra_logs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        TextView logsTextView = findViewById(R.id.logs_text_view);
        Button copyButton = findViewById(R.id.copy_button);
        Button restartButton = findViewById(R.id.restart_button);

        String logs = getIntent().getStringExtra(EXTRA_LOGS);
        logsTextView.setText(logs != null ? logs : "No logs available.");

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Logs", logs);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        restartButton.setOnClickListener(v -> {
            Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finishAffinity();
                System.exit(0);
            }
        });
    }
}
