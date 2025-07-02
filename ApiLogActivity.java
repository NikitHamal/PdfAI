package com.pdf.ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ApiLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiLogAdapter adapter;
    private ApiLogManager apiLogManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_log);

        apiLogManager = new ApiLogManager(this);
        recyclerView = findViewById(R.id.api_log_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ApiLogManager.ApiLog> logs = apiLogManager.getLogs();
        adapter = new ApiLogAdapter(logs);
        recyclerView.setAdapter(adapter);
    }
}