package com.pdf.ai.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.pdf.ai.PaintManager;

import com.pdf.ai.DrawUtils;

import java.util.List;
import java.util.regex.Pattern;

public class TextRenderer {

    private final PaintManager paintManager;

    public TextRenderer(PaintManager paintManager) {
        this.paintManager = paintManager;
    }

    public Object[] drawSectionTitle(PdfDocument document, PdfDocument.Page page, String title, float yPos, int currentPageNum) {
        // Implementation to be moved from PdfGenerator
        return new Object[]{page, yPos, currentPageNum};
    }

    public Object[] drawTextBlock(PdfDocument document, PdfDocument.Page page, String text, float yPos, int currentPageNum) {
        // Implementation to be moved from PdfGenerator
        return new Object[]{page, yPos, currentPageNum};
    }

    public Object[] simulateSectionTitle(String title, float yPos, int currentPageNum) {
        Paint paint = paintManager.getSectionTitlePaint();
        List<String> lines = DrawUtils.splitTextIntoLines(title, paint, PaintManager.CONTENT_WIDTH);
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

    public Object[] simulateTextBlock(String text, float yPos, int currentPageNum) {
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

            List<String> wrappedLines = DrawUtils.splitTextIntoLines(line, paintForBlock, effectiveContentWidth);
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
}
