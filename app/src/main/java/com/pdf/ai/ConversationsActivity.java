package com.pdf.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private RecyclerView conversationsRecyclerView;
    private RecyclerView pinnedConversationsRecyclerView;
    private ExtendedFloatingActionButton createNewFab;
    private View emptyState;
    private TextView pinnedHeader;
    private TextView recentHeader;
    private ConversationsAdapter conversationsAdapter;
    private ConversationsAdapter pinnedConversationsAdapter;
    private ConversationManager conversationManager;
    private List<ConversationManager.Conversation> conversations;
    private List<ConversationManager.Conversation> pinnedConversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        pinnedConversationsRecyclerView = findViewById(R.id.pinned_conversations_recycler_view);
        createNewFab = findViewById(R.id.create_new_fab);
        emptyState = findViewById(R.id.empty_state);
        pinnedHeader = findViewById(R.id.pinned_header);
        recentHeader = findViewById(R.id.recent_header);

        // Initialize ConversationManager
        conversationManager = new ConversationManager(this);
        conversations = new ArrayList<>();
        pinnedConversations = new ArrayList<>();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Load conversations
        loadConversations();

        createNewFab.setOnClickListener(v -> {
            startNewConversation();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh conversations list when returning to this activity
        loadConversations();
    }

    private void setupRecyclerViews() {
        // Pinned Conversations
        pinnedConversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pinnedConversationsAdapter = new ConversationsAdapter(this, pinnedConversations, this);
        pinnedConversationsRecyclerView.setAdapter(pinnedConversationsAdapter);

        // Recent Conversations
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversationsAdapter = new ConversationsAdapter(this, conversations, this);
        conversationsRecyclerView.setAdapter(conversationsAdapter);
    }

    private void loadConversations() {
        pinnedConversations = conversationManager.getPinnedConversations();
        conversations = conversationManager.getUnpinnedConversations();

        pinnedConversationsAdapter.updateConversations(pinnedConversations);
        conversationsAdapter.updateConversations(conversations);

        updateEmptyState();
        updateHeaders();
    }

    private void updateEmptyState() {
        if (conversations.isEmpty() && pinnedConversations.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            conversationsRecyclerView.setVisibility(View.GONE);
            pinnedConversationsRecyclerView.setVisibility(View.GONE);
            pinnedHeader.setVisibility(View.GONE);
            recentHeader.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
            pinnedConversationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateHeaders() {
        pinnedHeader.setVisibility(pinnedConversations.isEmpty() ? View.GONE : View.VISIBLE);
        recentHeader.setVisibility(conversations.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void startNewConversation() {
        // Clear current conversation state
        conversationManager.clearCurrentConversation();

        // Start new chat activity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("new_conversation", true);
        startActivity(intent);
    }

    @Override
    public void onConversationClick(ConversationManager.Conversation conversation) {
        // Set this conversation as current and open it
        conversationManager.setCurrentConversationId(conversation.id);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversation_id", conversation.id);
        startActivity(intent);
    }

    @Override
    public void onRenameClick(ConversationManager.Conversation conversation) {
        final EditText input = new EditText(this);
        input.setText(conversation.title);
        new AlertDialog.Builder(this)
                .setTitle("Rename Conversation")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        conversationManager.renameConversation(conversation.id, newTitle);
                        loadConversations();
                    } else {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onPinClick(ConversationManager.Conversation conversation) {
        conversationManager.setConversationPinned(conversation.id, !conversation.isPinned);
        loadConversations();
    }

    @Override
    public void onDeleteClick(ConversationManager.Conversation conversation) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = conversationManager.deleteConversation(conversation.id);
                    if (deleted) {
                        Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
                        loadConversations(); // Refresh the list
                    } else {
                        Toast.makeText(this, "Failed to delete conversation", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
