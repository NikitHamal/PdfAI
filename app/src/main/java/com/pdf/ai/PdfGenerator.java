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

    /**
     * FIX: This method has been completely rewritten to more accurately simulate the layout
     * process, ensuring page numbers in the Table of Contents are correct. It now mirrors
     * the actual drawing logic from ContentDrawer.
     */
    private void simulateLayoutAndBuildToc(OutlineData outlineData, List<String> sectionsContent) {
        tocItems.clear();
        int physicalPageNum = 3; // Content starts on physical page 3

        for (int i = 0; i < outlineData.getSections().size(); i++) {
            String sectionTitle = outlineData.getSections().get(i);
            String sectionContent = sectionsContent.get(i);

            // The drawing logic starts each section on a new page. The simulation must do the same.
            if (i > 0) {
                physicalPageNum++;
            }

            // Add TOC item for the start of the section. The logical page number is physical page - 2.
            tocItems.add(new TocItem(sectionTitle, physicalPageNum - 2, PaintManager.MARGIN));

            // Simulate drawing this section to find out how many pages it spans
            Object[] simResult = simulateSection(sectionTitle, sectionContent, PaintManager.MARGIN, physicalPageNum);
            physicalPageNum = (int) simResult[1]; // Update page number to the last page the section occupied
        }
    }

    /**
     * NEW: Simulates the layout of a full section to predict page breaks accurately.
     */
    private Object[] simulateSection(String sectionTitle, String sectionContent, float yPos, int currentPageNum) {
        // 1. Simulate Section Title
        Object[] titleResult = simulateSectionTitle(sectionTitle, yPos, currentPageNum);
        yPos = (float) titleResult[0];
        currentPageNum = (int) titleResult[1];

        // 2. Simulate Content Blocks
        String[] contentBlocks = sectionContent.split("\n\\s*\n");
        for (String block : contentBlocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty()) continue;

            // Avoid re-drawing the title if it's also in the content
            if (trimmedBlock.equals(sectionTitle) ||
                    trimmedBlock.startsWith("#") && trimmedBlock.substring(trimmedBlock.indexOf(" ") + 1).equals(sectionTitle)) {
                continue;
            }

            Object[] blockResult;
            if (trimmedBlock.startsWith("[[TABLE")) {
                blockResult = simulateTableBlock(trimmedBlock, yPos, currentPageNum);
            } else if (trimmedBlock.startsWith("[[CHART")) {
                blockResult = simulateVisualBlock(yPos, currentPageNum, 250); // Match drawChart chartHeight
            } else {
                blockResult = simulateTextBlock(trimmedBlock, yPos, currentPageNum);
            }
            yPos = (float) blockResult[0];
            currentPageNum = (int) blockResult[1];
        }
        return new Object[]{yPos, currentPageNum};
    }

    /**
     * NEW: Simulates the layout of a section title.
     */
    private Object[] simulateSectionTitle(String title, float yPos, int currentPageNum) {
        Paint paint = paintManager.getSectionTitlePaint();
        List<String> lines = ContentDrawer.splitTextIntoLines(title, paint, PaintManager.CONTENT_WIDTH);
        for (String line : lines) {
            float lineHeight = paint.descent() - paint.ascent();
            if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                currentPageNum++;
                yPos = PaintManager.MARGIN;
            }
            yPos += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }
        yPos += PaintManager.SECTION_TITLE_BOTTOM_MARGIN;
        return new Object[]{yPos, currentPageNum};
    }

    /**
     * NEW: Simulates the layout of a standard text block, including headings and lists.
     */
    private Object[] simulateTextBlock(String text, float yPos, int currentPageNum) {
        Paint paintForBlock;
        boolean isHeading = false;
        if (text.startsWith("###")) { paintForBlock = paintManager.getH3Paint(); text = text.substring(3).trim(); isHeading = true; }
        else if (text.startsWith("##")) { paintForBlock = paintManager.getH2Paint(); text = text.substring(2).trim(); isHeading = true; }
        else if (text.startsWith("#")) { paintForBlock = paintManager.getH1Paint(); text = text.substring(1).trim(); isHeading = true; }
        else { paintForBlock = paintManager.getTextPaint(); }

        yPos += isHeading ? PaintManager.HEADING_TOP_MARGIN : 0;

        String[] linesInBlock = text.split("\n");
        for (String line : linesInBlock) {
            if (line.trim().isEmpty()) continue;

            float effectiveContentWidth = PaintManager.CONTENT_WIDTH;
            Pattern bulletPattern = Pattern.compile("^[*-]\\s(.+)");
            Pattern numListPattern = Pattern.compile("^(\\d+)\\.\\s(.+)");
            if (bulletPattern.matcher(line.trim()).matches() || numListPattern.matcher(line.trim()).matches()) {
                effectiveContentWidth -= PaintManager.LIST_ITEM_INDENT;
            }

            List<String> wrappedLines = ContentDrawer.splitTextIntoLines(line, paintForBlock, effectiveContentWidth);
            for (String wrappedLine : wrappedLines) {
                float lineHeight = paintForBlock.descent() - paintForBlock.ascent();
                if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                    currentPageNum++;
                    yPos = PaintManager.MARGIN;
                }
                yPos += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
            }
        }
        yPos += PaintManager.PARAGRAPH_SPACING;
        return new Object[]{yPos, currentPageNum};
    }

    /**
     * NEW: Simulates the layout of a fixed-height visual block like a chart or table.
     */
    private Object[] simulateVisualBlock(float yPos, int currentPageNum, float height) {
        float titleHeight = (paintManager.getChartTitlePaint().descent() - paintManager.getChartTitlePaint().ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
        float totalHeight = PaintManager.VISUAL_TITLE_MARGIN + titleHeight + height + PaintManager.VISUAL_BOTTOM_MARGIN;

        if (yPos + totalHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
            currentPageNum++;
            yPos = PaintManager.MARGIN;
        }
        yPos += totalHeight;
        return new Object[]{yPos, currentPageNum};
    }

    /**
     * NEW: Simulates the layout of a table block, including title, header, rows, page breaks, and
     * repeating the header on new pages. Mirrors logic from ContentDrawer.drawTable/drawRow.
     */
    private Object[] simulateTableBlock(String tableString, float yPos, int currentPageNum) {
        try {
            // Parse the table description
            String raw = tableString.replace("[[TABLE|", "").replace("]]", "");
            String[] parts = raw.split("\\|");
            if (parts.length < 3) {
                // If format is invalid, assume small placeholder height
                return simulateVisualBlock(yPos, currentPageNum, 100);
            }

            String title = parts[0].trim();
            String[] headers = parts[1].split(",");
            List<String[]> rows = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                rows.add(parts[i].split(","));
            }

            // Before drawing the title, ContentDrawer ensures there is some minimum space (60)
            if (yPos + 60 > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                currentPageNum++;
                yPos = PaintManager.MARGIN;
            }

            // Title area
            yPos += PaintManager.VISUAL_TITLE_MARGIN;
            float titleLineHeight = (paintManager.getChartTitlePaint().descent() - paintManager.getChartTitlePaint().ascent())
                    * PaintManager.LINE_HEIGHT_MULTIPLIER;
            // Title may wrap; approximate using splitTextIntoLines for accuracy
            List<String> titleLines = ContentDrawer.splitTextIntoLines(title, paintManager.getChartTitlePaint(), PaintManager.CONTENT_WIDTH);
            if (titleLines.isEmpty()) {
                yPos += titleLineHeight; // fallback at least one line
            } else {
                yPos += titleLines.size() * titleLineHeight;
            }

            // Column widths: equal distribution as in ContentDrawer.calculateColumnWidths
            int numCols = Math.max(1, headers.length);
            float[] colWidths = new float[numCols];
            for (int i = 0; i < numCols; i++) {
                colWidths[i] = PaintManager.CONTENT_WIDTH / numCols;
            }

            // Helper to compute row height given data and paint
            java.util.function.BiFunction<String[], Paint, Float> computeRowHeight = (rowData, textPaint) -> {
                float cellPadding = PaintManager.CELL_PADDING;
                float maxTextHeight = 0f;
                for (int c = 0; c < numCols; c++) {
                    float colWidth = colWidths[c] - (2 * cellPadding);
                    String cell = (c < rowData.length) ? rowData[c].trim() : "";
                    List<String> wrapped = ContentDrawer.splitTextIntoLines(cell, textPaint, Math.max(0, colWidth));
                    float lineHeight = (textPaint.descent() - textPaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
                    float textHeight = wrapped.isEmpty() ? lineHeight : wrapped.size() * lineHeight;
                    if (textHeight > maxTextHeight) maxTextHeight = textHeight;
                }
                return maxTextHeight + (2 * cellPadding);
            };

            // Header row
            Paint headerPaint = paintManager.getTableHeaderPaint();
            float headerRowHeight = computeRowHeight.apply(headers, headerPaint);
            if (yPos + headerRowHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                currentPageNum++;
                yPos = PaintManager.MARGIN;
            }
            yPos += headerRowHeight;

            // Data rows
            Paint cellPaint = paintManager.getTableCellPaint();
            for (String[] row : rows) {
                float rowHeight = computeRowHeight.apply(row, cellPaint);
                if (yPos + rowHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                    // New page and repeat header
                    currentPageNum++;
                    yPos = PaintManager.MARGIN;

                    // Ensure header fits on the new page
                    if (yPos + headerRowHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                        // Extreme case: if even header doesn't fit, start another page
                        currentPageNum++;
                        yPos = PaintManager.MARGIN;
                    }
                    yPos += headerRowHeight;
                }
                yPos += rowHeight;
            }

            // Bottom margin after the table
            yPos += PaintManager.VISUAL_BOTTOM_MARGIN;

            return new Object[]{yPos, currentPageNum};
        } catch (Exception ex) {
            // On any parsing error, fallback to a generic visual block height
            return simulateVisualBlock(yPos, currentPageNum, 120);
        }
    }


    private void drawCoverPage(PdfDocument document, String title) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint titlePaint = paintManager.getTitlePaint();

        float centerX = PaintManager.PAGE_WIDTH / 2f;
        float centerY = PaintManager.PAGE_HEIGHT / 2f;
        float maxWidth = PaintManager.CONTENT_WIDTH - 80;

        List<String> titleLines = ContentDrawer.splitTextIntoLines(title, titlePaint, maxWidth);

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

            String truncatedTitle = ContentDrawer.truncateText(item.getTitle(), textPaint, availableWidth);
            canvas.drawText(truncatedTitle, PaintManager.MARGIN, yPosition, textPaint);
            canvas.drawText(pageNumStr, rightMargin, yPosition, numPaint);

            float titleWidth = textPaint.measureText(truncatedTitle);
            float startX = PaintManager.MARGIN + titleWidth + 5;
            float endX = rightMargin - numWidth - 5;
            if (startX < endX) {
                canvas.drawPath(ContentDrawer.createDottedLinePath(startX, endX, yPosition - (lineHeight / 4)), paintManager.getDottedLinePaint());
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
                contentDrawer.drawPageNumber(currentPage.getCanvas(), currentPage.getInfo().getPageNumber());
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
            contentDrawer.drawPageNumber(currentPage.getCanvas(), currentPage.getInfo().getPageNumber());
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
