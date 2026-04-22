package com.pdf.ai.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.R;
import com.pdf.ai.model.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    
    private final Context context;
    private List<Conversation> conversations;
    private final OnConversationClickListener listener;
    private final SimpleDateFormat dateFormat;
    
    public interface OnConversationClickListener {
        void onConversationClick(String conversationId);
        void onConversationDelete(String conversationId);
    }
    
    public ConversationAdapter(Context context, List<Conversation> conversations, OnConversationClickListener listener) {
        this.context = context;
        this.conversations = conversations;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    }
    
    public void updateData(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        
        holder.titleText.setText(conv.getTitle() != null ? conv.getTitle() : "New Chat");
        holder.previewText.setText(conv.getPreview());
        
        long updatedAt = conv.getUpdatedAt();
        long now = System.currentTimeMillis();
        long diff = now - updatedAt;
        
        String timeText;
        if (diff < 60000) {
            timeText = "Just now";
        } else if (diff < 3600000) {
            timeText = (diff / 60000) + "m ago";
        } else if (diff < 86400000) {
            timeText = (diff / 3600000) + "h ago";
        } else if (diff < 604800000) {
            timeText = (diff / 86400000) + "d ago";
        } else {
            timeText = dateFormat.format(new Date(updatedAt));
        }
        holder.timeText.setText(timeText);
        
        holder.messageCountText.setText(String.valueOf(conv.getMessageCount()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conv.getId());
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationDelete(conv.getId());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView previewText;
        TextView timeText;
        TextView messageCountText;
        ImageButton deleteButton;
        
        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.conversation_title);
            previewText = itemView.findViewById(R.id.conversation_preview);
            timeText = itemView.findViewById(R.id.conversation_time);
            messageCountText = itemView.findViewById(R.id.message_count);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}