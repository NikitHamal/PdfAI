package com.pdf.ai;

public class TocItem {
    private final String title;
    private final int pageNumber;
    private final float yPosition; // Y position on the page, for creating link targets

    public TocItem(String title, int pageNumber, float yPosition) {
        this.title = title;
        this.pageNumber = pageNumber;
        this.yPosition = yPosition;
    }

    public String getTitle() {
        return title;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public float getYPosition() {
        return yPosition;
    }
}
