package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRenderer {
    private final PaintManager paints;

    public TextRenderer(PaintManager paintManager) {
        this.paints = paintManager;
    }

    public Object[] drawSectionTitle(PdfDocument document, PdfDocument.Page page, String title, float yPos, int currentPageNum) {
        Paint paint = paints.getSectionTitlePaint();
        List<String> lines = DrawUtils.splitTextIntoLines(title, paint, PaintManager.CONTENT_WIDTH);
        for (String line : lines) {
            float lineHeight = paint.descent() - paint.ascent();
            if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                Object[] result = PageHelper.finishAndStartNewPage(document, page, currentPageNum, paints);
                page = (PdfDocument.Page) result[0];
                currentPageNum = (int) result[1];
                yPos = PaintManager.MARGIN;
            }
            page.getCanvas().drawText(line, PaintManager.MARGIN, yPos - paint.ascent(), paint);
            yPos += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }
        yPos += PaintManager.SECTION_TITLE_BOTTOM_MARGIN;
        return new Object[]{page, yPos, currentPageNum};
    }

    public Object[] drawTextBlock(PdfDocument document, PdfDocument.Page page, String text, float yPos, int currentPageNum) {
        Canvas canvas = page.getCanvas();
        Paint paintForBlock;
        boolean isHeading = false;

        if (text.startsWith("###")) {
            paintForBlock = paints.getH3Paint(); text = text.substring(3).trim(); isHeading = true;
        } else if (text.startsWith("##")) {
            paintForBlock = paints.getH2Paint(); text = text.substring(2).trim(); isHeading = true;
        } else if (text.startsWith("#")) {
            paintForBlock = paints.getH1Paint(); text = text.substring(1).trim(); isHeading = true;
        } else {
            paintForBlock = paints.getTextPaint();
        }

        yPos += isHeading ? PaintManager.HEADING_TOP_MARGIN : 0;

        String[] linesInBlock = text.split("\n");
        for (String line : linesInBlock) {
            if (line.trim().isEmpty()) continue;

            float currentX = PaintManager.MARGIN;
            String prefix = "";
            float effectiveContentWidth = PaintManager.CONTENT_WIDTH;
            Pattern bulletPattern = Pattern.compile("^[*-]\\s(.+)");
            Pattern numListPattern = Pattern.compile("^(\\d+)\\.\\s(.+)");
            Matcher bulletMatcher = bulletPattern.matcher(line.trim());
            Matcher numListMatcher = numListPattern.matcher(line.trim());

            String contentLine = line;
            if (bulletMatcher.matches()) {
                prefix = "• "; contentLine = bulletMatcher.group(1); currentX += PaintManager.LIST_ITEM_INDENT; effectiveContentWidth -= PaintManager.LIST_ITEM_INDENT;
            } else if (numListMatcher.matches()) {
                prefix = numListMatcher.group(1) + ". "; contentLine = numListMatcher.group(2); currentX += PaintManager.LIST_ITEM_INDENT; effectiveContentWidth -= PaintManager.LIST_ITEM_INDENT;
            }

            List<String> wrappedLines = DrawUtils.splitTextIntoLines(contentLine.trim(), paintForBlock, effectiveContentWidth);
            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                String wrappedLine = wrappedLines.get(lineIdx);
                float lineHeight = paintForBlock.descent() - paintForBlock.ascent();
                if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                    Object[] result = PageHelper.finishAndStartNewPage(document, page, currentPageNum, paints);
                    page = (PdfDocument.Page) result[0];
                    currentPageNum = (int) result[1];
                    canvas = page.getCanvas();
                    yPos = PaintManager.MARGIN;
                }
                if (lineIdx == 0 && !prefix.isEmpty()) {
                    canvas.drawText(prefix, PaintManager.MARGIN, yPos - paintForBlock.ascent(), paintForBlock);
                }
                drawStyledText(canvas, wrappedLine, currentX, yPos - paintForBlock.ascent(), isHeading ? paintForBlock : null);
                yPos += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
            }
        }
        yPos += PaintManager.PARAGRAPH_SPACING;
        return new Object[]{page, yPos, currentPageNum};
    }

    private void drawStyledText(Canvas canvas, String line, float x, float y, Paint headingPaint) {
        if (headingPaint != null) {
            canvas.drawText(line, x, y, headingPaint);
            return;
        }
        Pattern pattern = Pattern.compile("(\\*\\*[^\\*]+\\*\\*)|(\\*[^\\*]+\\*)");
        Matcher matcher = pattern.matcher(line);
        float currentX = x; int lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String plainText = line.substring(lastIndex, matcher.start());
                canvas.drawText(plainText, currentX, y, paints.getTextPaint());
                currentX += paints.getTextPaint().measureText(plainText);
            }
            String match = matcher.group();
            if (match.startsWith("**") && match.endsWith("**")) {
                String boldText = match.substring(2, match.length() - 2);
                canvas.drawText(boldText, currentX, y, paints.getBoldTextPaint());
                currentX += paints.getBoldTextPaint().measureText(boldText);
            } else if (match.startsWith("*") && match.endsWith("*")) {
                String italicText = match.substring(1, match.length() - 1);
                canvas.drawText(italicText, currentX, y, paints.getItalicTextPaint());
                currentX += paints.getItalicTextPaint().measureText(italicText);
            }
            lastIndex = matcher.end();
        }
        if (lastIndex < line.length()) {
            canvas.drawText(line.substring(lastIndex), currentX, y, paints.getTextPaint());
        }
    }
}
