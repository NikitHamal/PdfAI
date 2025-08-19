package com.pdf.ai.provider;

import com.pdf.ai.OutlineData;

public interface LLMProvider {
    interface OutlineCallback {
        void onSuccess(OutlineData outlineData);
        void onFailure(String error);
    }

    interface SectionCallback {
        void onSuccess(String sectionMarkdown);
        void onFailure(String error);
    }

    void generateOutline(String userPrompt, OutlineCallback callback);

    void generateSectionContent(String pdfTitle,
                                String sectionTitle,
                                java.util.List<String> allSections,
                                int currentSectionIndex,
                                SectionCallback callback);
}
