package com.pdf.ai.render;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.pdf.ai.PaintManager;

import android.graphics.pdf.PdfDocument;

public class ChartRenderer {

    private final PaintManager paintManager;

    public ChartRenderer(PaintManager paintManager) {
        this.paintManager = paintManager;
    }

    public Object[] drawChart(PdfDocument document, PdfDocument.Page page, String chartString, float yPos, int currentPageNum) {
        // Implementation to be moved from PdfGenerator/ContentDrawer
        return new Object[]{page, yPos, currentPageNum};
    }

    public Object[] simulateVisualBlock(float yPos, int currentPageNum, float height) {
        float titleHeight = (paintManager.getChartTitlePaint().descent() - paintManager.getChartTitlePaint().ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
        float totalHeight = PaintManager.VISUAL_TITLE_MARGIN + titleHeight + height + PaintManager.VISUAL_BOTTOM_MARGIN;

        if (yPos + totalHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
            currentPageNum++;
            yPos = PaintManager.MARGIN;
        }
        yPos += totalHeight;
        return new Object[]{yPos, currentPageNum};
    }
}
