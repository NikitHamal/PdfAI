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

            // 2. Draw content pages starting from page 3 to populate TOC items
            drawContentPages(document, outlineData, sectionsContent, contentDrawer, 3);

            // 3. Now draw the Table of Contents on page 2 with actual data
            insertTableOfContentsPage(document);

            // 4. Save the final document
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
        
        // Better title positioning - center both horizontally and vertically
        float centerX = PaintManager.PAGE_WIDTH / 2f;
        float centerY = PaintManager.PAGE_HEIGHT / 2f;
        float maxWidth = PaintManager.CONTENT_WIDTH - 80; // Extra margin for safety
        
        // Split title into lines and center them vertically
        List<String> titleLines = ContentDrawer.splitTextIntoLines(title, titlePaint, maxWidth);
        float totalHeight = titleLines.size() * (titlePaint.descent() - titlePaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
        float startY = centerY - (totalHeight / 2) - titlePaint.ascent();
        
        for (String line : titleLines) {
            canvas.drawText(line, centerX, startY, titlePaint);
            startY += (titlePaint.descent() - titlePaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }
        
        document.finishPage(page);
    }

    private int drawContentPages(PdfDocument document, OutlineData outlineData, List<String> sectionsContent, ContentDrawer contentDrawer, int startingPageNum) {
        int currentPageNumber = startingPageNum;
        PdfDocument.Page currentPage = null;
        float yPosition = PaintManager.MARGIN;

        for (int i = 0; i < outlineData.getSections().size(); i++) {
            String sectionTitle = outlineData.getSections().get(i);
            String sectionContent = sectionsContent.get(i);

            // Start each main section on a new page
            if (currentPage != null) {
                contentDrawer.drawPageNumber(currentPage.getCanvas(), currentPageNumber - 1);
                document.finishPage(currentPage);
            }
            
            // Create new page for each main section
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, currentPageNumber).create();
            currentPage = document.startPage(pageInfo);
            yPosition = PaintManager.MARGIN;
            
            // Add TOC entry for this main section with actual page number
            tocItems.add(new TocItem(sectionTitle, currentPageNumber, yPosition));

            Object[] result = contentDrawer.drawSection(document, currentPage, sectionTitle, sectionContent, yPosition, currentPageNumber, false);
            currentPage = (PdfDocument.Page) result[0];
            yPosition = (float) result[1];
            currentPageNumber = (int) result[2];
        }

        // Finish the last content page
        if (currentPage != null) {
            contentDrawer.drawPageNumber(currentPage.getCanvas(), currentPageNumber - 1);
            document.finishPage(currentPage);
        }
        
        return currentPageNumber - 1; // Return total pages used for content
    }

    private void insertTableOfContentsPage(PdfDocument document) {
        // Create the TOC page at position 2
        PdfDocument.PageInfo tocPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 2).create();
        PdfDocument.Page tocPage = document.startPage(tocPageInfo);
        drawTableOfContents(tocPage.getCanvas());
        document.finishPage(tocPage);
    }

    private void drawTableOfContents(Canvas canvas) {
        float yPosition = PaintManager.MARGIN + 30;

        // Draw TOC Title
        canvas.drawText("Table of Contents", PaintManager.PAGE_WIDTH / 2f, yPosition, paintManager.getTocTitlePaint());
        yPosition += 60;

        Paint tocTextPaint = paintManager.getTocTextPaint();
        Paint tocNumberPaint = paintManager.getTocNumberPaint();
        float lineHeight = tocTextPaint.descent() - tocTextPaint.ascent();

        for (TocItem item : tocItems) {
            if (yPosition > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN - 50) {
                // TOC is too long for one page
                break;
            }

            String pageNumStr = String.valueOf(item.getPageNumber());
            float numberWidth = tocNumberPaint.measureText(pageNumStr);

            // Draw dotted line
            String truncatedTitle = ContentDrawer.truncateText(item.getTitle(), tocTextPaint, PaintManager.CONTENT_WIDTH - numberWidth - 30);
            float titleWidth = tocTextPaint.measureText(truncatedTitle);
            float startX = PaintManager.MARGIN + titleWidth + 10;
            float endX = PaintManager.PAGE_WIDTH - PaintManager.MARGIN - numberWidth - 10;
            
            if (startX < endX) {
                canvas.drawPath(ContentDrawer.createDottedLinePath(startX, endX, yPosition - (lineHeight / 4)), paintManager.getDottedLinePaint());
            }

            // Draw section title
            canvas.drawText(truncatedTitle, PaintManager.MARGIN, yPosition, tocTextPaint);

            // Draw page number, right-aligned
            canvas.drawText(pageNumStr, PaintManager.PAGE_WIDTH - PaintManager.MARGIN, yPosition, tocNumberPaint);

            yPosition += lineHeight * 2.0f; // Increased spacing for better readability
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
