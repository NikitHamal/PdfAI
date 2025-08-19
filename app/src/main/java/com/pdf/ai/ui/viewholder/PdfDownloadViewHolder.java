package com.pdf.ai.ui.viewholder;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.pdf.ai.ChatMessage;
import com.pdf.ai.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDownloadViewHolder extends RecyclerView.ViewHolder {
    private final TextView pdfIntroTextView;
    private final TextView pdfFileNameTextView;
    private final ImageView downloadIcon;
    private final LinearLayout pdfDownloadContainer;

    public PdfDownloadViewHolder(@NonNull View itemView) {
        super(itemView);
        pdfIntroTextView = itemView.findViewById(R.id.pdf_intro_text_view);
        pdfFileNameTextView = itemView.findViewById(R.id.pdf_file_name_text_view);
        downloadIcon = itemView.findViewById(R.id.download_icon);
        pdfDownloadContainer = itemView.findViewById(R.id.pdf_download_container);
    }

    public void bind(ChatMessage message) {
        pdfIntroTextView.setText("Here's a comprehensive PDF on '" + message.getPdfTitle() + "':");
        pdfFileNameTextView.setText(new File(message.getFilePath()).getName());

        pdfDownloadContainer.setOnClickListener(v -> openPdf(message.getFilePath()));
        downloadIcon.setOnClickListener(v -> downloadPdf(message.getFilePath()));
    }

    private void openPdf(String filePath) {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            Toast.makeText(itemView.getContext(), "File not found. Please generate it again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri pdfUri = FileProvider.getUriForFile(itemView.getContext(), itemView.getContext().getPackageName() + ".provider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            itemView.getContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(itemView.getContext(), "No application available to view PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdf(String filePath) {
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            Toast.makeText(itemView.getContext(), "File not found. Please generate it again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.getName());
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = itemView.getContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = itemView.getContext().getContentResolver().openOutputStream(uri)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Toast.makeText(itemView.getContext(), "PDF downloaded to Downloads folder.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(itemView.getContext(), "Failed to download PDF.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(itemView.getContext(), "Failed to create new MediaStore record.", Toast.LENGTH_SHORT).show();
        }
    }
}
