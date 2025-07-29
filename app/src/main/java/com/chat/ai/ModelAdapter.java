package com.pdf.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelViewHolder> {

    private final List<Map.Entry<String, QwenApiClient.ModelInfo>> models;
    private final OnModelClickListener listener;

    public interface OnModelClickListener {
        void onModelClick(Map.Entry<String, QwenApiClient.ModelInfo> model);
    }

    public ModelAdapter(List<Map.Entry<String, QwenApiClient.ModelInfo>> models, OnModelClickListener listener) {
        this.models = models;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_selector, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        Map.Entry<String, QwenApiClient.ModelInfo> model = models.get(position);
        holder.bind(model, listener);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    static class ModelViewHolder extends RecyclerView.ViewHolder {
        private final TextView modelName;
        private final TextView modelDescription;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.model_name);
            modelDescription = itemView.findViewById(R.id.model_description);
        }

        public void bind(final Map.Entry<String, QwenApiClient.ModelInfo> model, final OnModelClickListener listener) {
            modelName.setText(model.getValue().displayName);
            modelDescription.setText(model.getValue().description);
            itemView.setOnClickListener(v -> listener.onModelClick(model));
        }
    }
}
