package com.pdf.ai.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.pdf.ai.ChatMessage;
import com.pdf.ai.R;

public class ProgressViewHolder extends RecyclerView.ViewHolder {
    private final LinearProgressIndicator progressBar;
    private final TextView statusTextView;

    public ProgressViewHolder(@NonNull View itemView) {
        super(itemView);
        progressBar = itemView.findViewById(R.id.progress_bar);
        statusTextView = itemView.findViewById(R.id.progress_status_text);
    }

    public void bind(ChatMessage message) {
        statusTextView.setText(message.getProgressStatus());
        progressBar.setProgress(message.getProgressValue());
    }
}
