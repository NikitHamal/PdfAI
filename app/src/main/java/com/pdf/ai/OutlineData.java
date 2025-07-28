package com.pdf.ai;

import java.util.List;

public class OutlineData {
    private String pdfTitle;
    private List<String> sections;

    public OutlineData(String pdfTitle, List<String> sections) {
        this.pdfTitle = pdfTitle;
        this.sections = sections;
    }

    public String getPdfTitle() {
        return pdfTitle;
    }

    public List<String> getSections() {
        return sections;
    }

    public void setPdfTitle(String pdfTitle) {
        this.pdfTitle = pdfTitle;
    }

    public void setSections(List<String> sections) {
        this.sections = sections;
    }
}