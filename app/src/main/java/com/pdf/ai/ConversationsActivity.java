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
    private ExtendedFloatingActionButton createNewFab;
    private View emptyState;
    private ConversationsAdapter conversationsAdapter;
    private ConversationManager conversationManager;
    private List<ConversationManager.Conversation> conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        createNewFab = findViewById(R.id.create_new_fab);
        emptyState = findViewById(R.id.empty_state);

        // Initialize ConversationManager
        conversationManager = new ConversationManager(this);
        conversations = new ArrayList<>();

        // Setup RecyclerView
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversationsAdapter = new ConversationsAdapter(this, conversations, this);
        conversationsRecyclerView.setAdapter(conversationsAdapter);

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

    private void loadConversations() {
        conversations = conversationManager.getAllConversations();
        conversationsAdapter.updateConversations(conversations);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (conversations.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            conversationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
        }
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
    public void onConversationLongClick(ConversationManager.Conversation conversation) {
        // Show delete dialog
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
