package com.chat.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {

    private RecyclerView conversationsRecyclerView;
    private ExtendedFloatingActionButton createNewFab;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        createNewFab = findViewById(R.id.create_new_fab);
        emptyState = findViewById(R.id.empty_state);

        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadConversations();

        createNewFab.setOnClickListener(v -> {
            // Clear the current conversation before starting a new one
            new ConversationManager(this).clearCurrentConversation();
            Intent intent = new Intent(this, ChatActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    private void loadConversations() {
        ConversationManager conversationManager = new ConversationManager(this);
        List<ConversationManager.Conversation> conversations = conversationManager.getAllConversations();

        if (conversations.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            conversationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
            ConversationsAdapter adapter = new ConversationsAdapter(conversations, conversation -> {
                conversationManager.saveCurrentConversationId(conversation.id);
                Intent intent = new Intent(this, ChatActivity.class);
                startActivityForResult(intent, 1);
            });
            conversationsRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            loadConversations();
        }
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
