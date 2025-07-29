package com.pdf.ai;

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
import com.pdf.ai.ConversationManager.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private RecyclerView conversationsRecyclerView;
    private ExtendedFloatingActionButton createNewFab;
    private View emptyState;
    private ConversationManager conversationManager;
    private ConversationsAdapter adapter;
    private List<Conversation> conversationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        createNewFab = findViewById(R.id.create_new_fab);
        emptyState = findViewById(R.id.empty_state);

        conversationManager = new ConversationManager(this);

        setupRecyclerView();

        createNewFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("is_new_chat", true);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }

    private void setupRecyclerView() {
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationsAdapter(conversationList, this);
        conversationsRecyclerView.setAdapter(adapter);
    }

    private void loadConversations() {
        conversationList.clear();
        conversationList.addAll(conversationManager.getAllConversations());
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (conversationList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            conversationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            conversationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversation.id);
        startActivity(intent);
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
