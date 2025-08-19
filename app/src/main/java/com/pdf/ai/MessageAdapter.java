package com.pdf.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.ui.interaction.OnOutlineActionListener;
import com.pdf.ai.ui.interaction.OnSuggestionClickListener;
import com.pdf.ai.ui.viewholder.AiMessageViewHolder;
import com.pdf.ai.ui.viewholder.OutlineViewHolder;
import com.pdf.ai.ui.viewholder.PdfDownloadViewHolder;
import com.pdf.ai.ui.viewholder.ProgressViewHolder;
import com.pdf.ai.ui.viewholder.SuggestionsViewHolder;
import com.pdf.ai.ui.viewholder.UserMessageViewHolder;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final List<ChatMessage> messages;
    private final OnOutlineActionListener outlineActionListener;
    private final OnSuggestionClickListener suggestionClickListener;

    public MessageAdapter(Context context, List<ChatMessage> messages, OnOutlineActionListener outlineActionListener, OnSuggestionClickListener suggestionClickListener) {
        this.context = context;
        this.messages = messages;
        this.outlineActionListener = outlineActionListener;
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
            case ChatMessage.TYPE_OUTLINE:
                view = inflater.inflate(R.layout.chat_item_outline, parent, false);
                return new OutlineViewHolder(view, outlineActionListener);
            case ChatMessage.TYPE_PDF_DOWNLOAD:
                view = inflater.inflate(R.layout.chat_item_pdf_download, parent, false);
                return new PdfDownloadViewHolder(view);
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
            case ChatMessage.TYPE_OUTLINE:
                ((OutlineViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_PDF_DOWNLOAD:
                ((PdfDownloadViewHolder) holder).bind(message);
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
}