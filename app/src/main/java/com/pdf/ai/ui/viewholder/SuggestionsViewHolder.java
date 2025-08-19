package com.pdf.ai.ui.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pdf.ai.ChatMessage;
import com.pdf.ai.R;
import com.pdf.ai.ui.interaction.OnSuggestionClickListener;

public class SuggestionsViewHolder extends RecyclerView.ViewHolder {
    private final MaterialCardView suggestion1, suggestion2, suggestion3, suggestion4;
    private final OnSuggestionClickListener listener;

    public SuggestionsViewHolder(@NonNull View itemView, OnSuggestionClickListener listener) {
        super(itemView);
        this.listener = listener;
        suggestion1 = itemView.findViewById(R.id.suggestion1);
        suggestion2 = itemView.findViewById(R.id.suggestion2);
        suggestion3 = itemView.findViewById(R.id.suggestion3);
        suggestion4 = itemView.findViewById(R.id.suggestion4);
    }

    public void bind(ChatMessage message) {
        suggestion1.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick("Create a PDF about the history of artificial intelligence.");
        });
        suggestion2.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick("Generate a PDF on the benefits of renewable energy.");
        });
        suggestion3.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick("Make a PDF about the importance of mental health.");
        });
        suggestion4.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick("Create a PDF on the art of storytelling.");
        });
    }
}
