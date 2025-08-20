package com.pdf.ai;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatManager {

    private final Context context;
    private final RecyclerView chatRecyclerView;
    private final List<ChatMessage> chatMessages;
    private final MessageAdapter messageAdapter;
    private final PreferencesManager preferencesManager;
    private OnUserMessageSentListener onUserMessageSentListener;

    public interface OnUserMessageSentListener {
        void onUserMessageSent(String message);
    }

    public ChatManager(Context context, RecyclerView chatRecyclerView, List<ChatMessage> chatMessages, MessageAdapter messageAdapter, PreferencesManager preferencesManager, OnUserMessageSentListener onUserMessageSentListener) {
        this.context = context;
        this.chatRecyclerView = chatRecyclerView;
        this.chatMessages = chatMessages;
        this.messageAdapter = messageAdapter;
        this.preferencesManager = preferencesManager;
        this.onUserMessageSentListener = onUserMessageSentListener;
    }

    public void sendUserMessage(String message) {
        if (chatMessages.size() == 1 && chatMessages.get(0).getType() == ChatMessage.TYPE_SUGGESTIONS) {
            chatMessages.remove(0);
            messageAdapter.notifyItemRemoved(0);
        }

        chatMessages.add(new ChatMessage(ChatMessage.TYPE_USER, message, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        if (onUserMessageSentListener != null) {
            onUserMessageSentListener.onUserMessageSent(message);
        }
    }

    public void addWelcomeMessage() {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_SUGGESTIONS, null, null, null));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public void showProgressMessage(String status, int progressValue) {
        if (chatMessages.isEmpty() || chatMessages.get(chatMessages.size() - 1).getType() != ChatMessage.TYPE_PROGRESS) {
            chatMessages.add(new ChatMessage(ChatMessage.TYPE_PROGRESS, null, status, progressValue, null));
            messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        } else {
            updateProgressMessage(status, progressValue);
        }
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public void updateProgressMessage(String status, int progressValue) {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                lastMessage.setProgressStatus(status);
                lastMessage.setProgressValue(progressValue);
                messageAdapter.notifyItemChanged(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        }
    }

    public void removeProgressMessage() {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (lastMessage.getType() == ChatMessage.TYPE_PROGRESS) {
                int removeIndex = chatMessages.size() - 1;
                chatMessages.remove(removeIndex);
                messageAdapter.notifyItemRemoved(removeIndex);
            }
        }
    }

    public void saveChatHistory() {
        preferencesManager.saveChatHistory(chatMessages);
    }

    public void loadChatHistory() {
        chatMessages.addAll(preferencesManager.loadChatHistory());
        messageAdapter.notifyDataSetChanged();
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    public void showOutlineInChat(OutlineData outlineData) {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_OUTLINE, null, null, outlineData));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public void removeOutlineFromChat() {
        for (int i = chatMessages.size() - 1; i >= 0; i--) {
            if (chatMessages.get(i).getType() == ChatMessage.TYPE_OUTLINE) {
                chatMessages.remove(i);
                messageAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    public void addPdfDownloadMessage(String pathOrUri, String pdfTitle) {
        chatMessages.add(new ChatMessage(ChatMessage.TYPE_PDF_DOWNLOAD, null, null, null, pathOrUri, pdfTitle));
        messageAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}
