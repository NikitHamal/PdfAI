package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.util.List;

public class ContentDrawer {

    private final PaintManager paints;
    // New modular renderers
    private final TextRenderer textRenderer;
    private final ChartRenderer chartRenderer;
    private final TableRenderer tableRenderer;

    public ContentDrawer(PaintManager paintManager) {
        this.paints = paintManager;
        this.textRenderer = new TextRenderer(paintManager);
        this.chartRenderer = new ChartRenderer(paintManager);
        this.tableRenderer = new TableRenderer(paintManager);
    }

    public Object[] drawSection(PdfDocument document, PdfDocument.Page page, String sectionTitle, String sectionContent, float yPos, int currentPageNum, boolean includeSectionTitle) {
        Object[] result = new Object[]{page, yPos, currentPageNum};
        float currentY = yPos;
        PdfDocument.Page currentPage = page;

        if (includeSectionTitle) {
            result = textRenderer.drawSectionTitle(document, currentPage, sectionTitle, currentY, currentPageNum);
            currentPage = (PdfDocument.Page) result[0];
            currentY = (float) result[1];
            currentPageNum = (int) result[2];
        }

        String[] contentBlocks = sectionContent.split("\n\\s*\n");
        for (String block : contentBlocks) {
            if (block.trim().isEmpty()) continue;
            String trimmedBlock = block.trim();

            if (trimmedBlock.equals(sectionTitle) ||
                    trimmedBlock.equals("# " + sectionTitle) ||
                    trimmedBlock.equals("## " + sectionTitle) ||
                    trimmedBlock.equals("### " + sectionTitle)) {
                continue;
            }

            if (trimmedBlock.startsWith("[[TABLE")) {
                result = tableRenderer.drawTable(document, currentPage, trimmedBlock, currentY, currentPageNum);
            } else if (trimmedBlock.startsWith("[[CHART")) {
                result = chartRenderer.drawChart(document, currentPage, trimmedBlock, currentY, currentPageNum);
            } else {
                result = textRenderer.drawTextBlock(document, currentPage, trimmedBlock, currentY, currentPageNum);
            }
            currentPage = (PdfDocument.Page) result[0];
            currentY = (float) result[1];
            currentPageNum = (int) result[2];
        }
        return new Object[]{currentPage, currentY, currentPageNum};
    }

    private Object[] drawSectionTitle(PdfDocument document, PdfDocument.Page page, String title, float yPos, int currentPageNum) {
        return textRenderer.drawSectionTitle(document, page, title, yPos, currentPageNum);
    }

    private Object[] drawTextBlock(PdfDocument document, PdfDocument.Page page, String text, float yPos, int currentPageNum) {
        return textRenderer.drawTextBlock(document, page, text, yPos, currentPageNum);
    }

    private Object[] drawChart(PdfDocument document, PdfDocument.Page page, String chartString, float yPos, int currentPageNum) {
        return chartRenderer.drawChart(document, page, chartString, yPos, currentPageNum);
    }

    private void drawBarChart(Canvas canvas, String[] parts, float yPos) {
        // Deprecated: Charts now handled by ChartRenderer
    }

    private void drawPieChart(Canvas canvas, String[] parts, float yPos) {
        // Deprecated: Charts now handled by ChartRenderer
    }

    private void drawLineChart(Canvas canvas, String[] parts, float yPos) {
        // Deprecated: Charts now handled by ChartRenderer
    }

    private void drawScatterPlot(Canvas canvas, String[] parts, float yPos) {
        // Deprecated: Charts now handled by ChartRenderer
    }

    private void drawCombinedChart(Canvas canvas, String[] parts, float yPos) {
        // Deprecated: Charts now handled by ChartRenderer
    }

    private void drawChartAxesAndGrid(Canvas canvas, float left, float top, float width, float height, List<Float> xData, List<Float> yData, String[] axisLabels) {
        // Deprecated: Charts now handled by ChartRenderer
    }
}
