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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfGenerator {

    private final Context context;
    private final PaintManager paintManager;
    private final TextRenderer textRenderer;
    private final TableRenderer tableRenderer;
    private final ChartRenderer chartRenderer;
    private final List<TocItem> tocItems = new ArrayList<>();

    public interface PdfGenerationCallback {
        void onPdfGenerated(String filePath, String pdfTitle);
        void onPdfGenerationFailed(String error);
    }

    public PdfGenerator(Context context) {
        this.context = context;
        this.paintManager = new PaintManager(context);
        this.textRenderer = new TextRenderer(paintManager);
        this.tableRenderer = new TableRenderer(paintManager);
        this.chartRenderer = new ChartRenderer(paintManager);
    }

    public void createPdf(String pdfTitle, OutlineData outlineData, List<String> sectionsContent, PdfGenerationCallback callback) {
        PdfDocument document = new PdfDocument();
        try {
            // Pass 1: Simulate the layout to get correct page numbers for the TOC
            simulateLayoutAndBuildToc(outlineData, sectionsContent);

            // Pass 2: Draw the actual document
            // Page 1: Cover Page
            drawCoverPage(document, pdfTitle);

            // Page 2: Table of Contents
            drawTableOfContentsPage(document);

            // Pages 3 onwards: Content
            drawAllContentPages(document, outlineData, sectionsContent);

            // Save the final document
            savePdf(document, pdfTitle, callback);

        } catch (Exception e) {
            Log.e("PdfGenerator", "Error during PDF creation process", e);
            callback.onPdfGenerationFailed("An unexpected error occurred: " + e.getMessage());
        } finally {
            document.close();
        }
    }

    private void simulateLayoutAndBuildToc(OutlineData outlineData, List<String> sectionsContent) {
        tocItems.clear();
        int physicalPageNum = 3; // Content starts on physical page 3

        for (int i = 0; i < outlineData.getSections().size(); i++) {
            String sectionTitle = outlineData.getSections().get(i);
            String sectionContent = sectionsContent.get(i);

            if (i > 0) {
                physicalPageNum++;
            }

            tocItems.add(new TocItem(sectionTitle, physicalPageNum - 2, PaintManager.MARGIN));

            Object[] simResult = simulateSection(sectionTitle, sectionContent, PaintManager.MARGIN, physicalPageNum);
            physicalPageNum = (int) simResult[1];
        }
    }

    private Object[] simulateSection(String sectionTitle, String sectionContent, float yPos, int currentPageNum) {
        Object[] titleResult = textRenderer.simulateSectionTitle(sectionTitle, yPos, currentPageNum);
        yPos = (float) titleResult[0];
        currentPageNum = (int) titleResult[1];

        String[] contentBlocks = sectionContent.split("\n\\s*\n");
        for (String block : contentBlocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty()) continue;

            if (trimmedBlock.equals(sectionTitle) ||
                    trimmedBlock.startsWith("#") && trimmedBlock.substring(trimmedBlock.indexOf(" ") + 1).equals(sectionTitle)) {
                continue;
            }

            Object[] blockResult;
            if (trimmedBlock.startsWith("[[TABLE")) {
                blockResult = tableRenderer.simulateTableBlock(trimmedBlock, yPos, currentPageNum);
            } else if (trimmedBlock.startsWith("[[CHART")) {
                blockResult = chartRenderer.simulateVisualBlock(yPos, currentPageNum, 250);
            } else {
                blockResult = textRenderer.simulateTextBlock(trimmedBlock, yPos, currentPageNum);
            }
            yPos = (float) blockResult[0];
            currentPageNum = (int) blockResult[1];
        }
        return new Object[]{yPos, currentPageNum};
    }


    private void drawCoverPage(PdfDocument document, String title) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint titlePaint = paintManager.getTitlePaint();

        float centerX = PaintManager.PAGE_WIDTH / 2f;
        float centerY = PaintManager.PAGE_HEIGHT / 2f;
        float maxWidth = PaintManager.CONTENT_WIDTH - 80;

        List<String> titleLines = DrawUtils.splitTextIntoLines(title, titlePaint, maxWidth);

        float lineHeight = (titlePaint.descent() - titlePaint.ascent());
        float totalHeight = (titleLines.size() * lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER) - (lineHeight * (PaintManager.LINE_HEIGHT_MULTIPLIER - 1.0f));

        float startY = centerY - (totalHeight / 2) - titlePaint.ascent();

        for (String line : titleLines) {
            canvas.drawText(line, centerX, startY, titlePaint);
            startY += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }

        document.finishPage(page);
    }

    private void drawTableOfContentsPage(PdfDocument document) {
        PdfDocument.PageInfo tocPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 2).create();
        PdfDocument.Page tocPage = document.startPage(tocPageInfo);
        drawTableOfContents(tocPage.getCanvas());
        document.finishPage(tocPage);
    }

    private void drawTableOfContents(Canvas canvas) {
        float yPosition = PaintManager.MARGIN + 30;
        canvas.drawText("Table of Contents", PaintManager.PAGE_WIDTH / 2f, yPosition, paintManager.getTocTitlePaint());
        yPosition += 60;

        Paint textPaint = paintManager.getTocTextPaint();
        Paint numPaint = paintManager.getTocNumberPaint();
        float lineHeight = textPaint.descent() - textPaint.ascent();
        float rightMargin = PaintManager.PAGE_WIDTH - PaintManager.MARGIN;

        for (TocItem item : tocItems) {
            if (yPosition > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN - 50) break;

            String pageNumStr = String.valueOf(item.getPageNumber());
            float numWidth = numPaint.measureText(pageNumStr);
            float availableWidth = rightMargin - PaintManager.MARGIN - numWidth - 20;

            String truncatedTitle = DrawUtils.truncateText(item.getTitle(), textPaint, availableWidth);
            canvas.drawText(truncatedTitle, PaintManager.MARGIN, yPosition, textPaint);
            canvas.drawText(pageNumStr, rightMargin, yPosition, numPaint);

            float titleWidth = textPaint.measureText(truncatedTitle);
            float startX = PaintManager.MARGIN + titleWidth + 5;
            float endX = rightMargin - numWidth - 5;
            if (startX < endX) {
                canvas.drawPath(DrawUtils.createDottedLinePath(startX, endX, yPosition - (lineHeight / 4)), paintManager.getDottedLinePaint());
            }

            yPosition += lineHeight * 2.0f;
        }
    }

    private void drawAllContentPages(PdfDocument document, OutlineData outlineData, List<String> sectionsContent) {
        ContentDrawer contentDrawer = new ContentDrawer(paintManager);
        int physicalPageNum = 3;
        PdfDocument.Page currentPage = null;

        for (int i = 0; i < outlineData.getSections().size(); i++) {
            if (currentPage != null) {
                PageHelper.drawPageNumber(currentPage.getCanvas(), currentPage.getInfo().getPageNumber(), paintManager);
                document.finishPage(currentPage);
            }

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, physicalPageNum).create();
            currentPage = document.startPage(pageInfo);

            Object[] result = contentDrawer.drawSection(document, currentPage,
                    outlineData.getSections().get(i),
                    sectionsContent.get(i),
                    PaintManager.MARGIN,
                    physicalPageNum,
                    true);

            currentPage = (PdfDocument.Page) result[0];
            // The next page number will be the current page's number + 1
            physicalPageNum = currentPage.getInfo().getPageNumber() + 1;
        }

        if (currentPage != null) {
            PageHelper.drawPageNumber(currentPage.getCanvas(), currentPage.getInfo().getPageNumber(), paintManager);
            document.finishPage(currentPage);
        }
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
        }
    }
}
