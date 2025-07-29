package com.pdf.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

public class CodeBlockView extends LinearLayout {

    private TextView languageLabel;
    private TextView codeText;
    private MaterialButton copyButton;
    private MaterialButton playButton;
    private String codeContent;
    private String language;

    public CodeBlockView(Context context) {
        super(context);
        init(context);
    }

    public CodeBlockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CodeBlockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.code_block_view, this, true);

        languageLabel = findViewById(R.id.language_label);
        codeText = findViewById(R.id.code_text);
        copyButton = findViewById(R.id.copy_button);
        playButton = findViewById(R.id.play_button);

        setupClickListeners();
    }

    private void setupClickListeners() {
        copyButton.setOnClickListener(v -> copyCodeToClipboard());
        playButton.setOnClickListener(v -> openInPlayground());
    }

    public void setCode(String code, String language) {
        this.codeContent = code;
        this.language = language;

        codeText.setText(code);
        languageLabel.setText(language != null ? language.toUpperCase() : "CODE");

        // Show play button for HTML code
        if ("html".equalsIgnoreCase(language)) {
            playButton.setVisibility(View.VISIBLE);
        } else {
            playButton.setVisibility(View.GONE);
        }
    }

    private void copyCodeToClipboard() {
        if (codeContent != null) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Code", codeContent);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Code copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInPlayground() {
        if ("html".equalsIgnoreCase(language) && codeContent != null) {
            try {
                // Create a temporary HTML file and open it in browser
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("data:text/html," + Uri.encode(codeContent)), "text/html");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Could not open HTML preview", Toast.LENGTH_SHORT).show();
            }
        }
    }
}