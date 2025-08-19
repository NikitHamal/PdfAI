package com.pdf.ai.ui.viewholder;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.pdf.ai.ChatMessage;
import com.pdf.ai.OutlineData;
import com.pdf.ai.R;
import com.pdf.ai.ui.interaction.OnOutlineActionListener;

public class OutlineViewHolder extends RecyclerView.ViewHolder {
    private final EditText pdfTitleEditText;
    private final LinearLayout sectionsContainer;
    private final MaterialButton addSectionButton;
    private final MaterialButton discardButton;
    private final MaterialButton approveButton;
    private final ImageView addTitleSectionButton;

    private final OnOutlineActionListener listener;
    private final Context context;

    public OutlineViewHolder(@NonNull View itemView, OnOutlineActionListener listener) {
        super(itemView);
        this.listener = listener;
        this.context = itemView.getContext();
        pdfTitleEditText = itemView.findViewById(R.id.pdf_title_edit_text);
        sectionsContainer = itemView.findViewById(R.id.outline_sections_container);
        addSectionButton = itemView.findViewById(R.id.add_section_button);
        discardButton = itemView.findViewById(R.id.discard_button);
        approveButton = itemView.findViewById(R.id.approve_button);
        addTitleSectionButton = itemView.findViewById(R.id.add_title_section);
    }

    public void bind(ChatMessage message) {
        OutlineData outlineData = message.getOutlineData();
        if (outlineData == null) return;

        pdfTitleEditText.setText(outlineData.getPdfTitle());
        sectionsContainer.removeAllViews();
        for (String section : outlineData.getSections()) {
            addSectionView(section, sectionsContainer, outlineData);
        }

        addTitleSectionButton.setOnClickListener(v -> {
            String newSectionTitle = "New Section";
            outlineData.getSections().add(0, newSectionTitle);
            sectionsContainer.removeAllViews();
            for (String section : outlineData.getSections()) {
                addSectionView(section, sectionsContainer, outlineData);
            }
        });

        addSectionButton.setOnClickListener(v -> {
            String newSectionTitle = "New Section";
            outlineData.getSections().add(newSectionTitle);
            addSectionView(newSectionTitle, sectionsContainer, outlineData);
        });

        pdfTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                outlineData.setPdfTitle(s.toString());
            }
        });

        discardButton.setOnClickListener(v -> {
            if (listener != null) listener.onDiscardOutline(getAdapterPosition());
        });
        approveButton.setOnClickListener(v -> {
            if (listener != null) listener.onApproveOutline(outlineData);
        });
    }

    private void addSectionView(String sectionTitle, LinearLayout container, OutlineData outlineData) {
        View sectionView = LayoutInflater.from(context).inflate(R.layout.outline_section_item, container, false);
        EditText sectionTitleEditText = sectionView.findViewById(R.id.section_title_edit_text);
        ImageView editButton = sectionView.findViewById(R.id.edit_section_button);
        ImageView deleteButton = sectionView.findViewById(R.id.delete_section_button);

        sectionTitleEditText.setText(sectionTitle);
        sectionTitleEditText.setEnabled(false);

        editButton.setOnClickListener(v -> {
            sectionTitleEditText.setEnabled(true);
            sectionTitleEditText.requestFocus();
            sectionTitleEditText.setSelection(sectionTitleEditText.getText().length());
        });

        sectionTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int index = container.indexOfChild(sectionView);
                if (index != -1) {
                    outlineData.getSections().set(index, s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        sectionTitleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                sectionTitleEditText.setEnabled(false);
            }
        });

        deleteButton.setOnClickListener(v -> {
            int index = container.indexOfChild(sectionView);
            if (index != -1) {
                container.removeView(sectionView);
                outlineData.getSections().remove(index);
            }
        });

        container.addView(sectionView);
    }
}
