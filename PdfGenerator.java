package com.pdf.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfGenerator {

    private final Context context;
    private final PaintManager paintManager;
    private final List<TocItem> tocItems = new ArrayList<>();

    public interface PdfGenerationCallback {
        void onPdfGenerated(String filePath, String pdfTitle);
        void onPdfGenerationFailed(String error);
    }

    public PdfGenerator(Context context) {
        this.context = context;
        this.paintManager = new PaintManager(context);
    }

    public void createPdf(String pdfTitle, OutlineData outlineData, List<String> sectionsContent, PdfGenerationCallback callback) {
        tocItems.clear();
        PdfDocument document = new PdfDocument();
        ContentDrawer contentDrawer = new ContentDrawer(paintManager, tocItems);

        try {
            // 1. Draw Cover Page
            drawCoverPage(document, pdfTitle);

            // 2. Reserve a page for TOC. We will draw on it later.
            PdfDocument.PageInfo tocPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 2).create();
            PdfDocument.Page tocPage = document.startPage(tocPageInfo);
            // We finish it empty for now. We'll draw on it later.
            document.finishPage(tocPage);

            // 3. Draw content pages, starting from page 3
            drawContentPages(document, outlineData, sectionsContent, contentDrawer, 3);

            // 4. Now, draw the actual Table of Contents on the reserved page (page 2)
            // Note: This is a simplified approach. True page insertion is complex.
            // We are essentially creating a placeholder and then drawing on it.
            // A more robust solution might involve creating two PDFs and merging.
            // Start the TOC page again to get its Canvas
            PdfDocument.PageInfo existingTocPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 2).create();
            PdfDocument.Page existingTocPage = document.startPage(existingTocPageInfo);
            drawTableOfContents(existingTocPage.getCanvas()); // Get canvas of page at index 1 (page 2)
            document.finishPage(existingTocPage); // Finish the TOC page after drawing

            // 5. Save the final document
            savePdf(document, pdfTitle, callback);

        } catch (Exception e) {
            Log.e("PdfGenerator", "Error during PDF creation process", e);
            callback.onPdfGenerationFailed("An unexpected error occurred: " + e.getMessage());
            if (document != null) {
                document.close();
            }
        }
    }

    private void drawCoverPage(PdfDocument document, String title) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint titlePaint = paintManager.getTitlePaint();
        float yPosition = PaintManager.PAGE_HEIGHT / 2.5f; // Centered vertically

        ContentDrawer.drawMultiLineText(canvas, title, PaintManager.PAGE_WIDTH / 2f, yPosition, titlePaint, PaintManager.CONTENT_WIDTH - 20);
        document.finishPage(page);
    }

    private void drawContentPages(PdfDocument document, OutlineData outlineData, List<String> sectionsContent, ContentDrawer contentDrawer, int startingPageNum) {
        int currentPageNumber = startingPageNum;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, currentPageNumber).create();
        PdfDocument.Page currentPage = document.startPage(pageInfo);
        float yPosition = PaintManager.MARGIN;

        for (int i = 0; i < outlineData.getSections().size(); i++) {
            String sectionTitle = outlineData.getSections().get(i);
            String sectionContent = sectionsContent.get(i);

            Object[] result = contentDrawer.drawSection(document, currentPage, sectionTitle, sectionContent, yPosition);
            currentPage = (PdfDocument.Page) result[0];
            yPosition = (float) result[1];
        }

        // Finish the last content page
        contentDrawer.drawPageNumber(currentPage.getCanvas(), document.getPages().size());
        document.finishPage(currentPage);
    }

    private void drawTableOfContents(Canvas canvas) {
        float yPosition = PaintManager.MARGIN;

        // Draw TOC Title
        canvas.drawText("Table of Contents", PaintManager.PAGE_WIDTH / 2f, yPosition + 20, paintManager.getTocTitlePaint());
        yPosition += 80;

        Paint tocTextPaint = paintManager.getTocTextPaint();
        Paint tocNumberPaint = paintManager.getTocNumberPaint();
        float lineHeight = tocTextPaint.descent() - tocTextPaint.ascent();

        for (TocItem item : tocItems) {
            if (yPosition > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                // TOC is too long for one page, which is an edge case not handled here.
                // For simplicity, we assume the TOC fits on a single page.
                break;
            }

            String pageNumStr = String.valueOf(item.getPageNumber());
            float numberWidth = tocNumberPaint.measureText(pageNumStr);

            // Draw dotted line
            String truncatedTitle = ContentDrawer.truncateText(item.getTitle(), tocTextPaint, PaintManager.CONTENT_WIDTH - numberWidth - 20);
            float titleWidth = tocTextPaint.measureText(truncatedTitle);
            float startX = PaintManager.MARGIN + titleWidth + 5;
            float endX = PaintManager.PAGE_WIDTH - PaintManager.MARGIN - numberWidth - 5;
            canvas.drawPath(ContentDrawer.createDottedLinePath(startX, endX, yPosition - (lineHeight / 4)), paintManager.getDottedLinePaint());

            // Draw section title
            canvas.drawText(truncatedTitle, PaintManager.MARGIN, yPosition, tocTextPaint);

            // Draw page number, right-aligned
            canvas.drawText(pageNumStr, PaintManager.PAGE_WIDTH - PaintManager.MARGIN, yPosition, tocNumberPaint);

            yPosition += lineHeight * 1.8f; // Increase spacing for readability
        }
        // Draw page number on TOC page
        paintManager.getPageNumberPaint().setTextAlign(Paint.Align.CENTER);
        canvas.drawText("2", PaintManager.PAGE_WIDTH / 2f, PaintManager.PAGE_HEIGHT - 20, paintManager.getPageNumberPaint());
    }

    private void savePdf(PdfDocument document, String pdfTitle, PdfGenerationCallback callback) {
        String fileName = pdfTitle.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        File pdfDir = new File(context.getCacheDir(), "pdfs");
        if (!pdfDir.exists() && !pdfDir.mkdirs()) {
             callback.onPdfGenerationFailed("Failed to create directory for PDF.");
             return;
        }
        File file = new File(pdfDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
            callback.onPdfGenerated(file.getAbsolutePath(), pdfTitle);
        } catch (IOException e) {
            Log.e("PdfGenerator", "Error saving PDF: " + e.getMessage(), e);
            callback.onPdfGenerationFailed(e.getMessage());
        } finally {
            document.close();
        }
    }
}
