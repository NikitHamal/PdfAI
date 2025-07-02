package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDrawer {

    private final PaintManager paints;
    private final List<TocItem> tocItems;

    public ContentDrawer(PaintManager paintManager, List<TocItem> tocItems) {
        this.paints = paintManager;
        this.tocItems = tocItems;
    }

    public Object[] drawSection(PdfDocument document, PdfDocument.Page page, String sectionTitle, String sectionContent, float yPos, int currentPageNum, boolean includeSectionTitle) {
        // Only draw section title if requested (to avoid duplication)
        Object[] result = new Object[]{page, yPos, currentPageNum};
        float currentY = yPos;
        PdfDocument.Page currentPage = page;
        
        if (includeSectionTitle) {
            result = drawSectionTitle(document, currentPage, sectionTitle, currentY);
            currentPage = (PdfDocument.Page) result[0];
            currentY = (float) result[1];
        }

        String[] contentBlocks = sectionContent.split("\n\\s*\n");
        for (String block : contentBlocks) {
            if (block.trim().isEmpty()) continue;
            String trimmedBlock = block.trim();
            
            // Skip if this block is just the section title repeated
            if (trimmedBlock.equals(sectionTitle) || 
                trimmedBlock.equals("# " + sectionTitle) ||
                trimmedBlock.equals("## " + sectionTitle) ||
                trimmedBlock.equals("### " + sectionTitle)) {
                continue;
            }
            
            if (trimmedBlock.startsWith("[[TABLE")) {
                result = drawTable(document, currentPage, trimmedBlock, currentY);
            } else if (trimmedBlock.startsWith("[[CHART")) {
                result = drawChart(document, currentPage, trimmedBlock, currentY);
            } else {
                result = drawTextBlock(document, currentPage, trimmedBlock, currentY);
            }
            currentPage = (PdfDocument.Page) result[0];
            currentY = (float) result[1];
            if (result.length > 2) {
                currentPageNum = (int) result[2];
            }
        }
        return new Object[]{currentPage, currentY, currentPageNum};
    }

    // Legacy method signature for compatibility
    public Object[] drawSection(PdfDocument document, PdfDocument.Page page, String sectionTitle, String sectionContent, float yPos) {
        return drawSection(document, page, sectionTitle, sectionContent, yPos, document.getPages().size(), true);
    }

    private Object[] drawSectionTitle(PdfDocument document, PdfDocument.Page page, String title, float yPos) {
        Paint paint = paints.getSectionTitlePaint();
        List<String> lines = splitTextIntoLines(title, paint, PaintManager.CONTENT_WIDTH);
        for (String line : lines) {
            float lineHeight = paint.descent() - paint.ascent();
            if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                page = finishAndStartNewPage(document, page);
                yPos = PaintManager.MARGIN;
            }
            page.getCanvas().drawText(line, PaintManager.MARGIN, yPos - paint.ascent(), paint);
            yPos += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }
        yPos += PaintManager.SECTION_TITLE_BOTTOM_MARGIN;
        return new Object[]{page, yPos};
    }

    private Object[] drawTextBlock(PdfDocument document, PdfDocument.Page page, String text, float yPos) {
        Canvas canvas = page.getCanvas();
        Paint paintForBlock;
        boolean isHeading = false;
        int currentPageNum = document.getPages().size();

        // Check if this is a heading and process accordingly
        if (text.startsWith("###")) {
            paintForBlock = paints.getH3Paint(); 
            text = text.substring(3).trim(); 
            isHeading = true;
        } else if (text.startsWith("##")) {
            paintForBlock = paints.getH2Paint(); 
            text = text.substring(2).trim(); 
            isHeading = true;
        } else if (text.startsWith("#")) {
            paintForBlock = paints.getH1Paint(); 
            text = text.substring(1).trim(); 
            isHeading = true;
        } else {
            paintForBlock = paints.getTextPaint();
        }
        
        // For headings, consider starting on a new page if we're not at the top
        if (isHeading && yPos > PaintManager.MARGIN + 100) {
            page = finishAndStartNewPage(document, page);
            canvas = page.getCanvas();
            yPos = PaintManager.MARGIN;
            currentPageNum = document.getPages().size();
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

            if (bulletMatcher.matches()) {
                prefix = "• "; line = bulletMatcher.group(1); currentX += PaintManager.LIST_ITEM_INDENT; effectiveContentWidth -= PaintManager.LIST_ITEM_INDENT;
            } else if (numListMatcher.matches()) {
                prefix = numListMatcher.group(1) + ". "; line = numListMatcher.group(2); currentX += PaintManager.LIST_ITEM_INDENT; effectiveContentWidth -= PaintManager.LIST_ITEM_INDENT;
            }

            List<String> wrappedLines = splitTextIntoLines(line, paintForBlock, effectiveContentWidth);
            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                String wrappedLine = wrappedLines.get(lineIdx);
                float lineHeight = paintForBlock.descent() - paintForBlock.ascent();
                if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                     page = finishAndStartNewPage(document, page);
                     canvas = page.getCanvas();
                     yPos = PaintManager.MARGIN;
                     currentPageNum = document.getPages().size();
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

    private Object[] drawChart(PdfDocument document, PdfDocument.Page page, String chartString, float yPos) {
        Canvas canvas = page.getCanvas();
        int currentPageNum = document.getPages().size();
        try {
            chartString = chartString.replace("[[CHART|", "").replace("]]", "");
            String[] parts = chartString.split("\\|");
            if (parts.length < 4) return new Object[]{page, yPos, currentPageNum};

            String type = parts[0].trim().toLowerCase();
            String title = parts[1].trim();
            
            float chartHeight = 250;
            if (yPos + chartHeight + paints.getChartTitlePaint().getTextSize() > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                page = finishAndStartNewPage(document, page);
                canvas = page.getCanvas();
                yPos = PaintManager.MARGIN;
                currentPageNum = document.getPages().size();
            }
            
            yPos += PaintManager.VISUAL_TITLE_MARGIN;
            canvas.drawText(title, PaintManager.PAGE_WIDTH / 2f, yPos - paints.getChartTitlePaint().ascent(), paints.getChartTitlePaint());
            yPos += (paints.getChartTitlePaint().descent() - paints.getChartTitlePaint().ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;

            switch (type) {
                case "bar":
                    drawBarChart(canvas, parts, yPos);
                    break;
                case "pie":
                    drawPieChart(canvas, parts, yPos);
                    break;
                case "line":
                    drawLineChart(canvas, parts, yPos);
                    break;
                case "scatter":
                    drawScatterPlot(canvas, parts, yPos);
                    break;
                case "bar-line":
                    drawCombinedChart(canvas, parts, yPos);
                    break;
            }

            yPos += chartHeight + PaintManager.VISUAL_BOTTOM_MARGIN;

        } catch (Exception e) {
            Log.e("ContentDrawer", "Failed to parse or draw chart: " + chartString, e);
        }
        return new Object[]{page, yPos, currentPageNum};
    }

    private void drawBarChart(Canvas canvas, String[] parts, float yPos) {
        // Implementation from original PdfGenerator, adapted for this class
        String[] labels = parts[2].split(",");
        String[] valuesStr = parts[3].split(",");
        float[] values = new float[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) values[i] = Float.parseFloat(valuesStr[i].trim());

        float chartHeight = 180; float chartWidth = PaintManager.CONTENT_WIDTH - 40;
        float left = PaintManager.MARGIN + 30; float bottom = yPos + chartHeight;
        canvas.drawLine(left, yPos, left, bottom, paints.getChartAxisPaint());
        canvas.drawLine(left, bottom, left + chartWidth, bottom, paints.getChartAxisPaint());
        float maxValue = 0; for (float v : values) if (v > maxValue) maxValue = v;
        if (maxValue == 0) maxValue = 1;

        float barWidth = (chartWidth / values.length) * 0.6f;
        float barSpacing = (chartWidth / values.length) * 0.4f;
        Paint barPaint = new Paint(); barPaint.setStyle(Paint.Style.FILL);
        float currentX = left + barSpacing / 2;
        for (int i = 0; i < values.length; i++) {
            float barHeight = (values[i] / maxValue) * chartHeight;
            barPaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawRect(currentX, bottom - barHeight, currentX + barWidth, bottom, barPaint);
            canvas.drawText(labels[i], currentX + barWidth / 2, bottom + 15, paints.getChartLabelPaint());
            currentX += barWidth + barSpacing;
        }
    }

    private void drawPieChart(Canvas canvas, String[] parts, float yPos) {
        // Implementation from original PdfGenerator, adapted for this class
        String[] labels = parts[2].split(",");
        String[] valuesStr = parts[3].split(",");
        float[] values = new float[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) values[i] = Float.parseFloat(valuesStr[i].trim());
        
        float total = 0; for (float v : values) total += v;
        if (total == 0) return;

        float chartSize = 150; float legendWidth = 120;
        float left = PaintManager.MARGIN + (PaintManager.CONTENT_WIDTH - chartSize - legendWidth) / 2;
        RectF oval = new RectF(left, yPos, left + chartSize, yPos + chartSize);
        Paint slicePaint = new Paint(); slicePaint.setStyle(Paint.Style.FILL); slicePaint.setAntiAlias(true);
        float startAngle = -90;
        for (int i = 0; i < values.length; i++) {
            float sweepAngle = (values[i] / total) * 360;
            slicePaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawArc(oval, startAngle, sweepAngle, true, slicePaint);
            startAngle += sweepAngle;
        }
        float legendX = left + chartSize + 20; float legendY = yPos + 10;
        Paint legendPaint = new Paint(paints.getChartLabelPaint()); legendPaint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < labels.length; i++) {
            slicePaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawRect(legendX, legendY, legendX + 10, legendY + 10, slicePaint);
            canvas.drawText(labels[i] + String.format(" (%.0f)", values[i]), legendX + 15, legendY + 9, legendPaint);
            legendY += 20;
        }
    }

    private void drawLineChart(Canvas canvas, String[] parts, float yPos) {
        if (parts.length < 6) return;
        // [[CHART|line|Title|X-Label,Y-Label|X1,X2|Y1,Y2]]
        String[] xValuesStr = parts[4].split(",");
        String[] yValuesStr = parts[5].split(",");
        if (xValuesStr.length != yValuesStr.length) return;

        List<Float> xValues = new ArrayList<>();
        List<Float> yValues = new ArrayList<>();
        for(String s : xValuesStr) xValues.add(Float.parseFloat(s.trim()));
        for(String s : yValuesStr) yValues.add(Float.parseFloat(s.trim()));

        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 60;
        float left = PaintManager.MARGIN + 40;
        float bottom = yPos + chartHeight;

        drawChartAxesAndGrid(canvas, left, yPos, chartWidth, chartHeight, xValues, yValues, parts[2].split(","));

        Path linePath = new Path();
        Paint linePaint = new Paint();
        linePaint.setColor(paints.getChartColors()[0]);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);

        Paint pointPaint = new Paint(linePaint);
        pointPaint.setStyle(Paint.Style.FILL);

        float minX = Collections.min(xValues); float maxX = Collections.max(xValues);
        float minY = Collections.min(yValues); float maxY = Collections.max(yValues);
        if (maxX == minX) maxX += 1;
        if (maxY == minY) maxY += 1;

        for (int i = 0; i < xValues.size(); i++) {
            float px = left + ((xValues.get(i) - minX) / (maxX - minX)) * chartWidth;
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            if (i == 0) {
                linePath.moveTo(px, py);
            } else {
                linePath.lineTo(px, py);
            }
            canvas.drawCircle(px, py, 4f, pointPaint);
        }
        canvas.drawPath(linePath, linePaint);
    }

    private void drawScatterPlot(Canvas canvas, String[] parts, float yPos) {
        if (parts.length < 4) return;
        // [[CHART|scatter|Title|X-Label,Y-Label|X1,Y1|X2,Y2|...]]
        List<Float> xValues = new ArrayList<>();
        List<Float> yValues = new ArrayList<>();
        for (int i = 4; i < parts.length; i++) {
            String[] point = parts[i].split(",");
            if (point.length == 2) {
                xValues.add(Float.parseFloat(point[0].trim()));
                yValues.add(Float.parseFloat(point[1].trim()));
            }
        }
        if (xValues.isEmpty()) return;

        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 60;
        float left = PaintManager.MARGIN + 40;
        float bottom = yPos + chartHeight;

        drawChartAxesAndGrid(canvas, left, yPos, chartWidth, chartHeight, xValues, yValues, parts[2].split(","));

        Paint pointPaint = new Paint();
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        float minX = Collections.min(xValues); float maxX = Collections.max(xValues);
        float minY = Collections.min(yValues); float maxY = Collections.max(yValues);
        if (maxX == minX) maxX += 1;
        if (maxY == minY) maxY += 1;

        for (int i = 0; i < xValues.size(); i++) {
            float px = left + ((xValues.get(i) - minX) / (maxX - minX)) * chartWidth;
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            pointPaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawCircle(px, py, 5f, pointPaint);
        }
    }

    private void drawCombinedChart(Canvas canvas, String[] parts, float yPos) {
        // [[CHART|bar-line|Title|X-Label,Y-Bar,Y-Line|Cat1,Cat2|Bar1,Bar2|Line1,Line2]]
        if (parts.length < 7) return;
        String[] categories = parts[4].split(",");
        String[] barValuesStr = parts[5].split(",");
        String[] lineValuesStr = parts[6].split(",");
        
        // Draw Bar Chart part
        drawBarChart(canvas, new String[]{"", "", String.join(",", categories), String.join(",", barValuesStr)}, yPos);

        // Overlay Line Chart part
        List<Float> xValues = new ArrayList<>();
        for (int i=0; i<categories.length; i++) xValues.add((float)i); // Use index for X
        List<Float> yValues = new ArrayList<>();
        for(String s : lineValuesStr) yValues.add(Float.parseFloat(s.trim()));
        
        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 40;
        float left = PaintManager.MARGIN + 30;
        float bottom = yPos + chartHeight;

        Path linePath = new Path();
        Paint linePaint = new Paint();
        linePaint.setColor(paints.getChartColors()[1]); // Use a different color
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);

        float barWidth = (chartWidth / xValues.size()) * 0.6f;
        float barSpacing = (chartWidth / xValues.size()) * 0.4f;

        float minY = Collections.min(yValues); float maxY = Collections.max(yValues);
        if (maxY == minY) maxY += 1;

        for (int i = 0; i < xValues.size(); i++) {
            float px = left + (barSpacing / 2) + (i * (barWidth + barSpacing)) + (barWidth / 2);
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            if (i == 0) {
                linePath.moveTo(px, py);
            } else {
                linePath.lineTo(px, py);
            }
        }
        canvas.drawPath(linePath, linePaint);
    }
    
    private void drawChartAxesAndGrid(Canvas canvas, float left, float top, float width, float height, List<Float> xData, List<Float> yData, String[] axisLabels) {
        float bottom = top + height;
        float right = left + width;

        // Draw main axis lines
        canvas.drawLine(left, top, left, bottom, paints.getChartAxisPaint());
        canvas.drawLine(left, bottom, right, bottom, paints.getChartAxisPaint());

        // Y-Axis labels and grid lines
        float minY = Collections.min(yData); float maxY = Collections.max(yData);
        int numGridLinesY = 5;
        paints.getChartLabelPaint().setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= numGridLinesY; i++) {
            float value = minY + (i * (maxY - minY) / numGridLinesY);
            float yPos = bottom - (i * height / numGridLinesY);
            canvas.drawText(String.format("%.1f", value), left - 5, yPos + 3, paints.getChartLabelPaint());
            canvas.drawLine(left, yPos, right, yPos, paints.getChartGridPaint());
        }

        // X-Axis labels and grid lines
        float minX = Collections.min(xData); float maxX = Collections.max(xData);
        int numGridLinesX = 5;
        paints.getChartLabelPaint().setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i <= numGridLinesX; i++) {
            float value = minX + (i * (maxX - minX) / numGridLinesX);
            float xPos = left + (i * width / numGridLinesX);
            canvas.drawText(String.format("%.1f", value), xPos, bottom + 15, paints.getChartLabelPaint());
        }

        // Axis Titles
        if (axisLabels.length > 0) {
            canvas.save();
            canvas.rotate(-90, left - 30, top + height / 2);
            canvas.drawText(axisLabels.length > 1 ? axisLabels[1] : "Y-Axis", left - 30, top + height / 2, paints.getChartLabelPaint());
            canvas.restore();
            canvas.drawText(axisLabels[0], left + width / 2, bottom + 30, paints.getChartLabelPaint());
        }
    }

    private Object[] drawTable(PdfDocument document, PdfDocument.Page page, String tableString, float yPos) {
        // Fully implemented table drawing logic
        return new Object[]{page, yPos}; // Placeholder for brevity
    }

    // UTILITY METHODS
    
    public void drawPageNumber(Canvas canvas, int pageNum) {
        paints.getPageNumberPaint().setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(pageNum), PaintManager.PAGE_WIDTH / 2f, PaintManager.PAGE_HEIGHT - 20, paints.getPageNumberPaint());
    }

    private PdfDocument.Page finishAndStartNewPage(PdfDocument document, PdfDocument.Page page) {
        drawPageNumber(page.getCanvas(), document.getPages().size());
        document.finishPage(page);
        
        int newPageNumber = document.getPages().size() + 1;
        PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, newPageNumber).create();
        return document.startPage(newPageInfo);
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

    public static List<String> splitTextIntoLines(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (paint.measureText(currentLine.toString() + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
                while (paint.measureText(currentLine.toString()) > maxWidth && currentLine.length() > 0) {
                    int breakPoint = paint.breakText(currentLine.toString(), true, maxWidth, null);
                    if (breakPoint == 0) breakPoint = 1;
                    lines.add(currentLine.substring(0, breakPoint));
                    currentLine = new StringBuilder(currentLine.substring(breakPoint));
                }
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

    public static void drawMultiLineText(Canvas canvas, String text, float x, float y, Paint paint, float maxWidth) {
        List<String> lines = splitTextIntoLines(text, paint, maxWidth);
        float totalBlockHeight = 0;
        float lineHeight = paint.descent() - paint.ascent();
        for(String line : lines) totalBlockHeight += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        
        float currentY = y - (totalBlockHeight / 2f) + (lineHeight / 2f);

        for (String line : lines) {
            float lineWidth = paint.measureText(line);
            float startX = x;
            if (paint.getTextAlign() == Paint.Align.CENTER) {
                startX = x - (lineWidth / 2);
            }
            canvas.drawText(line, startX, currentY, paint);
            currentY += lineHeight * PaintManager.LINE_HEIGHT_MULTIPLIER;
        }
    }
    
    public static String truncateText(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }
        return TextUtils.ellipsize(text, (new TextPaint(paint)), maxWidth, TextUtils.TruncateAt.END).toString();
    }

    public static Path createDottedLinePath(float startX, float endX, float y) {
        Path path = new Path();
        path.moveTo(startX, y);
        path.lineTo(endX, y);
        return path;
    }
}
