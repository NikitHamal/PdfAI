package com.pdf.ai.render;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.pdf.ai.PaintManager;

import android.graphics.pdf.PdfDocument;

import com.pdf.ai.DrawUtils;

import java.util.ArrayList;
import java.util.List;

public class TableRenderer {

    private final PaintManager paintManager;

    public TableRenderer(PaintManager paintManager) {
        this.paintManager = paintManager;
    }

    public Object[] drawTable(PdfDocument document, PdfDocument.Page page, String tableString, float yPos, int currentPageNum) {
        // Implementation to be moved from PdfGenerator/ContentDrawer
        return new Object[]{page, yPos, currentPageNum};
    }

    public Object[] simulateTableBlock(String tableString, float yPos, int currentPageNum) {
        try {
            // Parse the table description
            String raw = tableString.replace("[[TABLE|", "").replace("]]", "");
            String[] parts = raw.split("\\|");
            if (parts.length < 3) {
                // If format is invalid, assume small placeholder height
                return new Object[]{yPos + 100, currentPageNum};
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
            List<String> titleLines = DrawUtils.splitTextIntoLines(title, paintManager.getChartTitlePaint(), PaintManager.CONTENT_WIDTH);
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
                    List<String> wrapped = DrawUtils.splitTextIntoLines(cell, textPaint, Math.max(0, colWidth));
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
            return new Object[]{yPos + 120, currentPageNum};
        }
    }
}
