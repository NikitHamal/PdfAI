package com.pdf.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private Context context;
    private List<ConversationManager.Conversation> conversations;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ConversationManager.Conversation conversation);
        void onRenameClick(ConversationManager.Conversation conversation);
        void onPinClick(ConversationManager.Conversation conversation);
        void onDeleteClick(ConversationManager.Conversation conversation);
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
        private TextView dateTextView;
        private ImageView pinIcon;
        private ImageButton moreOptionsButton;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleTextView = itemView.findViewById(R.id.conversation_title);
            dateTextView = itemView.findViewById(R.id.conversation_date);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            moreOptionsButton = itemView.findViewById(R.id.more_options_button);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationClick(conversations.get(position));
                }
            });

            moreOptionsButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    showPopupMenu(moreOptionsButton, conversations.get(position));
                }
            });
        }

        void bind(ConversationManager.Conversation conversation) {
            titleTextView.setText(conversation.title != null ? conversation.title : "New Chat");
            dateTextView.setText(DateUtils.getRelativeTimeSpanString(conversation.lastModified));
            pinIcon.setVisibility(conversation.isPinned ? View.VISIBLE : View.GONE);
        }

        private void showPopupMenu(View view, ConversationManager.Conversation conversation) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_conversation_options, popup.getMenu());

            // Set pin/unpin title
            MenuItem pinItem = popup.getMenu().findItem(R.id.action_pin);
            pinItem.setTitle(conversation.isPinned ? "Unpin" : "Pin");

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_rename) {
                    listener.onRenameClick(conversation);
                    return true;
                } else if (itemId == R.id.action_pin) {
                    listener.onPinClick(conversation);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onDeleteClick(conversation);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}