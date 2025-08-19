package com.pdf.ai.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.ChatMessage;
import com.pdf.ai.R;

public class AiMessageViewHolder extends RecyclerView.ViewHolder {
    private final TextView messageTextView;

    public AiMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        messageTextView = itemView.findViewById(R.id.ai_message_text_view);
    }

    public void bind(ChatMessage message) {
        messageTextView.setText(message.getMessage());
    }
}
