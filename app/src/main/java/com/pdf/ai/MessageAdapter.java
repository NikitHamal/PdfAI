package com.pdf.ai;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.pdf.ai.R;
import com.pdf.ai.ChatMessage;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<ChatMessage> messages;
    private OnSuggestionClickListener suggestionClickListener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String prompt);
    }

    public MessageAdapter(Context context, List<ChatMessage> messages, OnSuggestionClickListener suggestionClickListener) {
        this.context = context;
        this.messages = messages;
        this.suggestionClickListener = suggestionClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case ChatMessage.TYPE_USER:
                view = inflater.inflate(R.layout.chat_message_layout_user, parent, false);
                return new UserMessageViewHolder(view);
            case ChatMessage.TYPE_AI:
                view = inflater.inflate(R.layout.chat_message_layout_ai, parent, false);
                return new AiMessageViewHolder(view);
            case ChatMessage.TYPE_PROGRESS:
                view = inflater.inflate(R.layout.chat_item_progress, parent, false);
                return new ProgressViewHolder(view);
            case ChatMessage.TYPE_SUGGESTIONS:
                view = inflater.inflate(R.layout.chat_item_suggestions, parent, false);
                return new SuggestionsViewHolder(view, suggestionClickListener);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        switch (message.getType()) {
            case ChatMessage.TYPE_USER:
                ((UserMessageViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_AI:
                ((AiMessageViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_PROGRESS:
                ((ProgressViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_SUGGESTIONS:
                ((SuggestionsViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.user_message_text);
        }

        void bind(ChatMessage message) {
            messageTextView.setText(message.getMessage());
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        MaterialCardView thinkingCard;
        MaterialCardView webSearchCard;
        TextView thinkingContent;
        TextView webSearchContent;
        LinearLayout thinkingHeader;
        LinearLayout webSearchHeader;
        ImageView thinkingExpandIcon;
        ImageView webSearchExpandIcon;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.ai_message_text);
            thinkingCard = itemView.findViewById(R.id.thinking_card);
            webSearchCard = itemView.findViewById(R.id.web_search_card);
            thinkingContent = itemView.findViewById(R.id.thinking_content);
            webSearchContent = itemView.findViewById(R.id.web_search_content);
            thinkingHeader = itemView.findViewById(R.id.thinking_header);
            webSearchHeader = itemView.findViewById(R.id.web_search_header);
            thinkingExpandIcon = itemView.findViewById(R.id.thinking_expand_icon);
            webSearchExpandIcon = itemView.findViewById(R.id.web_search_expand_icon);
        }

        void bind(ChatMessage message) {
            messageTextView.setText(message.getMessage());

            // Handle thinking content
            if (message.hasThinking()) {
                thinkingCard.setVisibility(View.VISIBLE);
                thinkingContent.setText(message.getThinkingContent());
                
                thinkingHeader.setOnClickListener(v -> {
                    boolean isExpanded = thinkingContent.getVisibility() == View.VISIBLE;
                    thinkingContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                    thinkingExpandIcon.setRotation(isExpanded ? 0 : 180);
                });
            } else {
                thinkingCard.setVisibility(View.GONE);
            }

            // Handle web search content
            if (message.hasWebSearch()) {
                webSearchCard.setVisibility(View.VISIBLE);
                webSearchContent.setText(message.getWebSearchContent());
                
                webSearchHeader.setOnClickListener(v -> {
                    boolean isExpanded = webSearchContent.getVisibility() == View.VISIBLE;
                    webSearchContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                    webSearchExpandIcon.setRotation(isExpanded ? 0 : 180);
                });
            } else {
                webSearchCard.setVisibility(View.GONE);
            }
        }
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        LinearProgressIndicator progressBar;
        TextView statusTextView;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            statusTextView = itemView.findViewById(R.id.status_text);
        }

        void bind(ChatMessage message) {
            statusTextView.setText(message.getProgressStatus());
            progressBar.setProgress(message.getProgressValue());
        }
    }

    class SuggestionsViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView suggestion1, suggestion2, suggestion3, suggestion4;
        OnSuggestionClickListener listener;

        public SuggestionsViewHolder(@NonNull View itemView, OnSuggestionClickListener listener) {
            super(itemView);
            this.listener = listener;
            suggestion1 = itemView.findViewById(R.id.suggestion_1);
            suggestion2 = itemView.findViewById(R.id.suggestion_2);
            suggestion3 = itemView.findViewById(R.id.suggestion_3);
            suggestion4 = itemView.findViewById(R.id.suggestion_4);
        }

        void bind(ChatMessage message) {
            // Parse suggestions from message content
            String[] suggestions = message.getMessage().split("\n");
            
            setupSuggestion(suggestion1, suggestions.length > 0 ? suggestions[0] : "", 0);
            setupSuggestion(suggestion2, suggestions.length > 1 ? suggestions[1] : "", 1);
            setupSuggestion(suggestion3, suggestions.length > 2 ? suggestions[2] : "", 2);
            setupSuggestion(suggestion4, suggestions.length > 3 ? suggestions[3] : "", 3);
        }

        private void setupSuggestion(MaterialCardView cardView, String suggestion, int index) {
            if (suggestion.isEmpty()) {
                cardView.setVisibility(View.GONE);
                return;
            }

            cardView.setVisibility(View.VISIBLE);
            TextView textView = cardView.findViewById(R.id.suggestion_text);
            textView.setText(suggestion);

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick(suggestion);
                }
            });
        }
    }
}