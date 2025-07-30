package com.pdf.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import io.noties.markwon.Markwon;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<ChatMessage> messages;
    private OnSuggestionClickListener suggestionClickListener;
    private Markwon markwon;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String prompt);
    }

    public MessageAdapter(Context context, List<ChatMessage> messages, OnSuggestionClickListener suggestionClickListener, Markwon markwon) {
        this.context = context;
        this.messages = messages;
        this.suggestionClickListener = suggestionClickListener;
        this.markwon = markwon;
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
            case ChatMessage.TYPE_SUGGESTIONS:
                ((SuggestionsViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolders
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.user_message_text_view);
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
            messageTextView = itemView.findViewById(R.id.ai_message_text_view);
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
            markwon.setMarkdown(messageTextView, message.getMessage());

            // Handle thinking content
            if (message.hasThinking()) {
                thinkingCard.setVisibility(View.VISIBLE);
                thinkingContent.setText(message.getThinkingContent());
                
                thinkingHeader.setOnClickListener(v -> {
                    boolean isExpanded = thinkingContent.getVisibility() == View.VISIBLE;
                    thinkingContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                    thinkingExpandIcon.setRotation(isExpanded ? 0f : 180f);
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
                    webSearchExpandIcon.setRotation(isExpanded ? 0f : 180f);
                });
            } else {
                webSearchCard.setVisibility(View.GONE);
            }
        }
    }


    class SuggestionsViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView suggestionCard1, suggestionCard2, suggestionCard3, suggestionCard4;
        TextView suggestionText1, suggestionText2, suggestionText3, suggestionText4;
        OnSuggestionClickListener listener;

        public SuggestionsViewHolder(@NonNull View itemView, OnSuggestionClickListener listener) {
            super(itemView);
            this.listener = listener;
            suggestionCard1 = itemView.findViewById(R.id.suggestion1);
            suggestionCard2 = itemView.findViewById(R.id.suggestion2);
            suggestionCard3 = itemView.findViewById(R.id.suggestion3);
            suggestionCard4 = itemView.findViewById(R.id.suggestion4);
            suggestionText1 = itemView.findViewById(R.id.suggestion_text1);
            suggestionText2 = itemView.findViewById(R.id.suggestion_text2);
            suggestionText3 = itemView.findViewById(R.id.suggestion_text3);
            suggestionText4 = itemView.findViewById(R.id.suggestion_text4);
        }

        void bind(ChatMessage message) {
            String q1 = "Who won the last world cup?";
            String q2 = "Explain quantum computing in simple terms";
            String q3 = "What are some healthy dinner recipes?";
            String q4 = "Write a short story about a friendly robot";

            suggestionText1.setText(q1);
            suggestionText2.setText(q2);
            suggestionText3.setText(q3);
            suggestionText4.setText(q4);

            suggestionCard1.setOnClickListener(v -> {
                if (listener != null) listener.onSuggestionClick(q1);
            });
            suggestionCard2.setOnClickListener(v -> {
                if (listener != null) listener.onSuggestionClick(q2);
            });
            suggestionCard3.setOnClickListener(v -> {
                if (listener != null) listener.onSuggestionClick(q3);
            });
            suggestionCard4.setOnClickListener(v -> {
                if (listener != null) listener.onSuggestionClick(q4);
            });
        }
    }
}