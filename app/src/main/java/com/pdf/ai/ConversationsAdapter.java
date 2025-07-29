package com.pdf.ai;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {
    
    private Context context;
    private List<ConversationManager.Conversation> conversations;
    private OnConversationClickListener listener;
    
    public interface OnConversationClickListener {
        void onConversationClick(ConversationManager.Conversation conversation);
        void onConversationLongClick(ConversationManager.Conversation conversation);
    }
    
    public ConversationsAdapter(Context context, List<ConversationManager.Conversation> conversations, OnConversationClickListener listener) {
        this.context = context;
        this.conversations = conversations;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationManager.Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }
    
    @Override
    public int getItemCount() {
        return conversations.size();
    }
    
    public void updateConversations(List<ConversationManager.Conversation> newConversations) {
        this.conversations = newConversations;
        notifyDataSetChanged();
    }
    
    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView titleTextView;
        private TextView lastMessageTextView;
        private TextView dateTextView;
        private TextView messageCountTextView;
        
        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleTextView = itemView.findViewById(R.id.conversation_title);
            lastMessageTextView = itemView.findViewById(R.id.conversation_last_message);
            
            // Add these views to the layout if they don't exist
            dateTextView = itemView.findViewById(R.id.conversation_date);
            messageCountTextView = itemView.findViewById(R.id.conversation_message_count);
            
            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationClick(conversations.get(position));
                }
            });
            
            cardView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationLongClick(conversations.get(position));
                    return true;
                }
                return false;
            });
        }
        
        void bind(ConversationManager.Conversation conversation) {
            titleTextView.setText(conversation.title != null ? conversation.title : "New Chat");
            
            // Get the last message for preview
            String lastMessage = "No messages yet";
            if (conversation.messages != null && !conversation.messages.isEmpty()) {
                for (int i = conversation.messages.size() - 1; i >= 0; i--) {
                    ChatMessage message = conversation.messages.get(i);
                    if (message.getType() == ChatMessage.TYPE_USER && !message.getMessage().isEmpty()) {
                        lastMessage = message.getMessage();
                        break;
                    }
                }
            }
            
            // Truncate long messages
            if (lastMessage.length() > 100) {
                lastMessage = lastMessage.substring(0, 97) + "...";
            }
            
            lastMessageTextView.setText(lastMessage);
            
            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(conversation.lastModified));
            
            if (dateTextView != null) {
                dateTextView.setText(dateStr);
            }
            
            if (messageCountTextView != null) {
                int messageCount = conversation.getMessageCount();
                messageCountTextView.setText(messageCount + " message" + (messageCount != 1 ? "s" : ""));
            }
        }
    }
}