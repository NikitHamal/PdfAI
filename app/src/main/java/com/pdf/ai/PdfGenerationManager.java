package com.pdf.ai;

import android.content.Context;

import java.util.List;

public class PdfGenerationManager {

    private final Context context;
    private final LLMProvider llmProvider;
    private final PdfGenerator pdfGenerator;
    private final ChatManager chatManager;

    public PdfGenerationManager(Context context, LLMProvider llmProvider, PdfGenerator pdfGenerator, ChatManager chatManager) {
        this.context = context;
        this.llmProvider = llmProvider;
        this.pdfGenerator = pdfGenerator;
        this.chatManager = chatManager;
    }

    public void startPdfGeneration(OutlineData approvedOutline) {
        List<String> currentPdfSectionsContent = new ArrayList<>();
        // This should be run on a background thread
        // For simplicity, we are keeping it on the main thread for now
        // A better approach would be to use an ExecutorService
        ((MainActivity) context).runOnUiThread(() -> chatManager.showProgressMessage("Writing content for: " + approvedOutline.getSections().get(0) + " (0%)", 0));
        generateSectionContent(approvedOutline, 0, currentPdfSectionsContent);
    }

    private void generateSectionContent(OutlineData outlineData, int sectionIndex, List<String> currentPdfSectionsContent) {
        if (sectionIndex >= outlineData.getSections().size()) {
            ((MainActivity) context).runOnUiThread(() -> {
                chatManager.updateProgressMessage("All content generated. Finalizing PDF...", 100);
            });

            pdfGenerator.createPdf(outlineData.getPdfTitle(), outlineData, currentPdfSectionsContent, new PdfGenerator.PdfGenerationCallback() {
                @Override
                public void onPdfGenerated(String pathOrUri, String pdfTitle) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        chatManager.removeProgressMessage();
                        String cleanedTitle = pdfTitle.replace("A comprehensive", "").trim();
                        Toast.makeText(context, "PDF created successfully", Toast.LENGTH_LONG).show();
                        chatManager.addPdfDownloadMessage(pathOrUri, cleanedTitle);
                        chatManager.saveChatHistory();
                    });
                }

                @Override
                public void onPdfGenerationFailed(String error) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        chatManager.removeProgressMessage();
                        Toast.makeText(context, "Error creating PDF: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
            return;
        }

        String sectionTitle = outlineData.getSections().get(sectionIndex);
        int totalSections = outlineData.getSections().size();
        int progress = (int) (((float) sectionIndex / totalSections) * 100);

        ((MainActivity) context).runOnUiThread(() -> chatManager.updateProgressMessage("Writing content for: " + sectionTitle + " (" + progress + "%)", progress));

        llmProvider.generateSectionContent(outlineData.getPdfTitle(), sectionTitle, outlineData.getSections(), sectionIndex, new LLMProvider.SectionCallback() {
            @Override
            public void onSuccess(String sectionMarkdown) {
                ((MainActivity) context).runOnUiThread(() -> {
                    String cleanedContent = MarkdownParser.normalize(sectionMarkdown);
                    currentPdfSectionsContent.add(cleanedContent);
                    generateSectionContent(outlineData, sectionIndex + 1, currentPdfSectionsContent);
                });
            }

            @Override
            public void onFailure(String error) {
                ((MainActivity) context).runOnUiThread(() -> {
                    chatManager.updateProgressMessage("Error generating content for " + sectionTitle + ": " + error, progress);
                });
            }
        });
    }
}
