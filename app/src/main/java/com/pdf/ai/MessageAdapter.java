package com.pdf.ai;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator; // Import LinearProgressIndicator
import com.pdf.ai.R;
import com.pdf.ai.ChatMessage;
import com.pdf.ai.OutlineData;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.util.List;
import java.io.File;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.os.Environment;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<ChatMessage> messages;
    private OnOutlineActionListener outlineActionListener;
    private OnSuggestionClickListener suggestionClickListener;

    public interface OnOutlineActionListener {
        void onApproveOutline(OutlineData outlineData);
        void onDiscardOutline(int position);
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String prompt);
    }

    public MessageAdapter(Context context, List<ChatMessage> messages, OnOutlineActionListener outlineActionListener, OnSuggestionClickListener suggestionClickListener) {
        this.context = context;
        this.messages = messages;
        this.outlineActionListener = outlineActionListener;
        this.suggestionClickListener = suggestionClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case ChatMessage.TYPE_USER:
                view = inflater.inflate(R.layout.chat_message_layout_user, parent, false);
                return new UserMessageViewHolder(view);
            case ChatMessage.TYPE_AI:
                view = inflater.inflate(R.layout.chat_message_layout_ai, parent, false);
                return new AiMessageViewHolder(view);
            case ChatMessage.TYPE_PROGRESS:
                view = inflater.inflate(R.layout.chat_item_progress, parent, false);
                return new ProgressViewHolder(view);
            case ChatMessage.TYPE_OUTLINE:
                view = inflater.inflate(R.layout.chat_item_outline, parent, false);
                return new OutlineViewHolder(view, outlineActionListener);
            case ChatMessage.TYPE_PDF_DOWNLOAD:
                view = inflater.inflate(R.layout.chat_item_pdf_download, parent, false);
                return new PdfDownloadViewHolder(view);
            case ChatMessage.TYPE_SUGGESTIONS:
                view = inflater.inflate(R.layout.chat_item_suggestions, parent, false);
                return new SuggestionsViewHolder(view, suggestionClickListener);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        switch (message.getType()) {
            case ChatMessage.TYPE_USER:
                ((UserMessageViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_AI:
                ((AiMessageViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_PROGRESS:
                ((ProgressViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_OUTLINE:
                ((OutlineViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_PDF_DOWNLOAD:
                ((PdfDownloadViewHolder) holder).bind(message);
                break;
            case ChatMessage.TYPE_SUGGESTIONS:
                ((SuggestionsViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolders
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.user_message_text_view);
        }

        void bind(ChatMessage message) {
            messageTextView.setText(message.getMessage() != null ? message.getMessage() : "");
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.ai_message_text_view);
        }

        void bind(ChatMessage message) {
            messageTextView.setText(message.getMessage() != null ? message.getMessage() : "");
        }
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        LinearProgressIndicator progressBar; // Changed from ProgressBar to LinearProgressIndicator
        TextView statusTextView;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            statusTextView = itemView.findViewById(R.id.progress_status_text);
        }

        void bind(ChatMessage message) {
            statusTextView.setText(message.getProgressStatus() != null ? message.getProgressStatus() : "");
            progressBar.setProgress(message.getProgressValue()); // Set the progress value
        }
    }

    class OutlineViewHolder extends RecyclerView.ViewHolder {
        EditText pdfTitleEditText;
        LinearLayout sectionsContainer;
        MaterialButton addSectionButton;
        MaterialButton discardButton;
        MaterialButton approveButton;
        ImageView addTitleSectionButton;

        OnOutlineActionListener listener;

        public OutlineViewHolder(@NonNull View itemView, OnOutlineActionListener listener) {
            super(itemView);
            this.listener = listener;
            pdfTitleEditText = itemView.findViewById(R.id.pdf_title_edit_text);
            sectionsContainer = itemView.findViewById(R.id.outline_sections_container);
            addSectionButton = itemView.findViewById(R.id.add_section_button);
            discardButton = itemView.findViewById(R.id.discard_button);
            approveButton = itemView.findViewById(R.id.approve_button);
            addTitleSectionButton = itemView.findViewById(R.id.add_title_section);
        }

        void bind(ChatMessage message) {
            OutlineData outlineData = message.getOutlineData();
            if (outlineData != null) {
                pdfTitleEditText.setText(outlineData.getPdfTitle());
                sectionsContainer.removeAllViews();
                for (String section : outlineData.getSections()) {
                    addSectionView(section, sectionsContainer, outlineData);
                }

                addTitleSectionButton.setOnClickListener(v -> {
                    String newSectionTitle = "New Section";
                    outlineData.getSections().add(0, newSectionTitle); // Add at the beginning for title sections
                    sectionsContainer.removeAllViews(); // Re-add all sections to reflect change
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
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        outlineData.setPdfTitle(s.toString());
                    }
                });

                discardButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDiscardOutline(getAdapterPosition());
                    }
                });

                approveButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onApproveOutline(outlineData);
                    }
                });
            }
        }

        private void addSectionView(String sectionTitle, LinearLayout container, OutlineData outlineData) {
            View sectionView = LayoutInflater.from(context).inflate(R.layout.outline_section_item, container, false);
            EditText sectionTitleEditText = sectionView.findViewById(R.id.section_title_edit_text);
            ImageView editButton = sectionView.findViewById(R.id.edit_section_button);
            ImageView deleteButton = sectionView.findViewById(R.id.delete_section_button);

            sectionTitleEditText.setText(sectionTitle);
            sectionTitleEditText.setEnabled(false); // Initially disabled

            // Edit button functionality
            editButton.setOnClickListener(v -> {
                sectionTitleEditText.setEnabled(true);
                sectionTitleEditText.requestFocus();
                sectionTitleEditText.setSelection(sectionTitleEditText.getText().length());
            });

            // Save changes on focus loss or text change
            sectionTitleEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int index = container.indexOfChild(sectionView);
                    if (index != -1) {
                        outlineData.getSections().set(index, s.toString());
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            sectionTitleEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    sectionTitleEditText.setEnabled(false);
                }
            });

            // Delete button functionality
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

    class PdfDownloadViewHolder extends RecyclerView.ViewHolder {
        TextView pdfIntroTextView;
        TextView pdfFileNameTextView;
        ImageView downloadIcon;
        LinearLayout pdfDownloadContainer;

        public PdfDownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfIntroTextView = itemView.findViewById(R.id.pdf_intro_text_view);
            pdfFileNameTextView = itemView.findViewById(R.id.pdf_file_name_text_view);
            downloadIcon = itemView.findViewById(R.id.download_icon); // Changed from view_icon
            pdfDownloadContainer = itemView.findViewById(R.id.pdf_download_container);
        }

        void bind(ChatMessage message) {
            pdfIntroTextView.setText("Here's a comprehensive PDF on '" + message.getPdfTitle() + "':");
            pdfFileNameTextView.setText(new File(message.getFilePath()).getName());

            // Click the layout to open the PDF
            pdfDownloadContainer.setOnClickListener(v -> openPdf(message.getFilePath()));
            // Click the icon to download the PDF
            downloadIcon.setOnClickListener(v -> downloadPdf(message.getFilePath()));
        }

        private void openPdf(String filePath) {
            File pdfFile = new File(filePath);
            if (!pdfFile.exists()) {
                Toast.makeText(context, "File not found. Please generate it again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use FileProvider to create a content URI for secure sharing
            Uri pdfUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); // Optional: activity won't be kept in history stack

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "No application available to view PDF", Toast.LENGTH_SHORT).show();
            }
        }

        private void downloadPdf(String filePath) {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                Toast.makeText(context, "File not found. Please generate it again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use MediaStore to save to the public Downloads folder
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    Toast.makeText(context, "PDF downloaded to Downloads folder.", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(context, "Failed to download PDF.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to create new MediaStore record.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class SuggestionsViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView suggestion1, suggestion2, suggestion3, suggestion4;
        OnSuggestionClickListener listener;

        public SuggestionsViewHolder(@NonNull View itemView, OnSuggestionClickListener listener) {
            super(itemView);
            this.listener = listener;
            suggestion1 = itemView.findViewById(R.id.suggestion1);
            suggestion2 = itemView.findViewById(R.id.suggestion2);
            suggestion3 = itemView.findViewById(R.id.suggestion3);
            suggestion4 = itemView.findViewById(R.id.suggestion4);
        }

        void bind(ChatMessage message) {
            suggestion1.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick("Create a PDF about the history of artificial intelligence.");
                }
            });
            suggestion2.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick("Generate a PDF on the benefits of renewable energy.");
                }
            });
            suggestion3.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick("Make a PDF about the importance of mental health.");
                }
            });
            suggestion4.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick("Create a PDF on the art of storytelling.");
                }
            });
        }
    }
}