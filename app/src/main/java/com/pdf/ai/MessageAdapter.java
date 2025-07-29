package com.pdf.ai;

import android.content.Context;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.syntax.highlight.PrismHighlightPlugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;
    private static final int VIEW_TYPE_SUGGESTIONS = 2;

    private Context context;
    private List<ChatMessage> messages;
    private OnSuggestionClickListener suggestionClickListener;
    private Markwon markwon;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    public MessageAdapter(Context context, List<ChatMessage> messages, OnSuggestionClickListener suggestionClickListener) {
        this.context = context;
        this.messages = messages;
        this.suggestionClickListener = suggestionClickListener;
        
        // Initialize Markwon for markdown rendering
        this.markwon = Markwon.builder(context)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(PrismHighlightPlugin.create())
                .build();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        switch (viewType) {
            case VIEW_TYPE_USER:
                View userView = inflater.inflate(R.layout.chat_message_layout_user, parent, false);
                return new UserMessageViewHolder(userView);
            case VIEW_TYPE_AI:
                View aiView = inflater.inflate(R.layout.chat_message_layout_ai, parent, false);
                return new AiMessageViewHolder(aiView);
            case VIEW_TYPE_SUGGESTIONS:
                View suggestionsView = inflater.inflate(R.layout.chat_item_suggestions, parent, false);
                return new SuggestionsViewHolder(suggestionsView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER:
                ((UserMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_AI:
                ((AiMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_SUGGESTIONS:
                ((SuggestionsViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        switch (message.getType()) {
            case ChatMessage.TYPE_USER:
                return VIEW_TYPE_USER;
            case ChatMessage.TYPE_AI:
                return VIEW_TYPE_AI;
            case ChatMessage.TYPE_SUGGESTIONS:
                return VIEW_TYPE_SUGGESTIONS;
            default:
                return VIEW_TYPE_AI;
        }
    }

    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
        }
    }

    class AiMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView thinkingText;
        private TextView webSearchText;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.ai_message_text);
            thinkingText = itemView.findViewById(R.id.thinking_text);
            webSearchText = itemView.findViewById(R.id.web_search_text);
            
            // Enable links in text
            messageText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        void bind(ChatMessage message) {
            String content = message.getDisplayContent();
            
            // Process content for code blocks and markdown
            if (content != null && !content.isEmpty()) {
                // Extract and replace code blocks with custom views
                String processedContent = processCodeBlocks(content);
                
                // Render markdown
                Spanned spanned = markwon.toMarkdown(processedContent);
                messageText.setText(spanned);
            } else {
                messageText.setText("");
            }
            
            // Show thinking content if available
            if (message.hasThinking() && message.getThinkingContent() != null) {
                thinkingText.setVisibility(View.VISIBLE);
                thinkingText.setText("🤔 " + message.getThinkingContent());
            } else {
                thinkingText.setVisibility(View.GONE);
            }
            
            // Show web search content if available
            if (message.hasWebSearch() && message.getWebSearchContent() != null) {
                webSearchText.setVisibility(View.VISIBLE);
                webSearchText.setText("🔍 " + message.getWebSearchContent());
            } else {
                webSearchText.setVisibility(View.GONE);
            }
        }
        
        private String processCodeBlocks(String content) {
            // Pattern to match code blocks: ```language\ncode\n```
            Pattern codeBlockPattern = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
            Matcher matcher = codeBlockPattern.matcher(content);
            
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String language = matcher.group(1);
                String code = matcher.group(2);
                
                // Replace with HTML that will be rendered as custom code block
                String replacement = "<div class='code-block' data-language='" + (language != null ? language : "") + "'>" + 
                                  "<pre><code>" + escapeHtml(code) + "</code></pre></div>";
                matcher.appendReplacement(result, replacement);
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
        
        private String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\"", "&quot;")
                      .replace("'", "&#39;");
        }
    }

    class SuggestionsViewHolder extends RecyclerView.ViewHolder {
        private View suggestion1, suggestion2, suggestion3, suggestion4;
        private TextView suggestionText1, suggestionText2, suggestionText3, suggestionText4;

        SuggestionsViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestion1 = itemView.findViewById(R.id.suggestion_1);
            suggestion2 = itemView.findViewById(R.id.suggestion_2);
            suggestion3 = itemView.findViewById(R.id.suggestion_3);
            suggestion4 = itemView.findViewById(R.id.suggestion_4);
            
            suggestionText1 = itemView.findViewById(R.id.suggestion_text_1);
            suggestionText2 = itemView.findViewById(R.id.suggestion_text_2);
            suggestionText3 = itemView.findViewById(R.id.suggestion_text_3);
            suggestionText4 = itemView.findViewById(R.id.suggestion_text_4);

            setupClickListeners();
        }

        private void setupClickListeners() {
            suggestion1.setOnClickListener(v -> {
                if (suggestionClickListener != null) {
                    suggestionClickListener.onSuggestionClick(suggestionText1.getText().toString());
                }
            });

            suggestion2.setOnClickListener(v -> {
                if (suggestionClickListener != null) {
                    suggestionClickListener.onSuggestionClick(suggestionText2.getText().toString());
                }
            });

            suggestion3.setOnClickListener(v -> {
                if (suggestionClickListener != null) {
                    suggestionClickListener.onSuggestionClick(suggestionText3.getText().toString());
                }
            });

            suggestion4.setOnClickListener(v -> {
                if (suggestionClickListener != null) {
                    suggestionClickListener.onSuggestionClick(suggestionText4.getText().toString());
                }
            });
        }

        void bind(ChatMessage message) {
            // Set suggestion texts dynamically based on message content
            String[] suggestions = message.getMessage().split("\\|");
            if (suggestions.length >= 1) suggestionText1.setText(suggestions[0].trim());
            if (suggestions.length >= 2) suggestionText2.setText(suggestions[1].trim());
            if (suggestions.length >= 3) suggestionText3.setText(suggestions[2].trim());
            if (suggestions.length >= 4) suggestionText4.setText(suggestions[3].trim());
        }
    }
}