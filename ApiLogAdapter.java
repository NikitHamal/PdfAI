package com.pdf.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton; // Import MaterialButton

import java.util.List;

public class ApiLogAdapter extends RecyclerView.Adapter<ApiLogAdapter.ViewHolder> {

    private List<ApiLogManager.ApiLog> logs;
    private Context context; // Add context to access clipboard manager

    public ApiLogAdapter(List<ApiLogManager.ApiLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get context from parent for Toast and ClipboardManager
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.api_log_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiLogManager.ApiLog log = logs.get(position);
        holder.promptTextView.setText("Prompt: " + log.getPrompt());
        holder.responseTextView.setText("Response: " + log.getResponse());

        // Set OnClickListener for the copy button
        holder.copyButton.setOnClickListener(v -> {
            // Combine prompt and response for copying
            String textToCopy = "Prompt: " + log.getPrompt() + "\n\nResponse: " + log.getResponse();

            // Copy text to clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("API Log", textToCopy);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Log copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to copy log.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView promptTextView;
        public TextView responseTextView;
        public MaterialButton copyButton; // Reference for the new copy button

        public ViewHolder(View itemView) {
            super(itemView);
            promptTextView = itemView.findViewById(R.id.log_prompt_text);
            responseTextView = itemView.findViewById(R.id.log_response_text);
            copyButton = itemView.findViewById(R.id.copy_log_button); // Initialize the copy button
        }
    }
}
