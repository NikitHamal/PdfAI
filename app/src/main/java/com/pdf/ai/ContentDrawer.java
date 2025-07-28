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

    public ContentDrawer(PaintManager paintManager) {
        this.paints = paintManager;
    }

    public Object[] drawSection(PdfDocument document, PdfDocument.Page page, String sectionTitle, String sectionContent, float yPos, int currentPageNum, boolean includeSectionTitle) {
        Object[] result = new Object[]{page, yPos, currentPageNum};
        float currentY = yPos;
        PdfDocument.Page currentPage = page;

        if (includeSectionTitle) {
            result = drawSectionTitle(document, currentPage, sectionTitle, currentY, currentPageNum);
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
                result = drawTable(document, currentPage, trimmedBlock, currentY, currentPageNum);
            } else if (trimmedBlock.startsWith("[[CHART")) {
                result = drawChart(document, currentPage, trimmedBlock, currentY, currentPageNum);
            } else {
                result = drawTextBlock(document, currentPage, trimmedBlock, currentY, currentPageNum);
            }
            currentPage = (PdfDocument.Page) result[0];
            currentY = (float) result[1];
            currentPageNum = (int) result[2];
        }
        return new Object[]{currentPage, currentY, currentPageNum};
    }

    private Object[] drawSectionTitle(PdfDocument document, PdfDocument.Page page, String title, float yPos, int currentPageNum) {
        Paint paint = paints.getSectionTitlePaint();
        List<String> lines = splitTextIntoLines(title, paint, PaintManager.CONTENT_WIDTH);
        for (String line : lines) {
            float lineHeight = paint.descent() - paint.ascent();
            if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                Object[] result = finishAndStartNewPage(document, page, currentPageNum);
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

    private Object[] drawTextBlock(PdfDocument document, PdfDocument.Page page, String text, float yPos, int currentPageNum) {
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

            List<String> wrappedLines = splitTextIntoLines(contentLine.trim(), paintForBlock, effectiveContentWidth);
            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                String wrappedLine = wrappedLines.get(lineIdx);
                float lineHeight = paintForBlock.descent() - paintForBlock.ascent();
                if (yPos + lineHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                    Object[] result = finishAndStartNewPage(document, page, currentPageNum);
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

    /**
     * FIX: This method and its sub-methods have been updated to be more resilient to
     * formatting errors in the data received from the AI model. It now handles
     * extra commas and invalid numbers gracefully.
     */
    private Object[] drawChart(PdfDocument document, PdfDocument.Page page, String chartString, float yPos, int currentPageNum) {
        Canvas canvas = page.getCanvas();
        try {
            chartString = chartString.replace("[[CHART|", "").replace("]]", "");
            String[] parts = chartString.split("\\|");
            if (parts.length < 4) throw new IllegalArgumentException("Invalid chart format: Not enough parts.");

            String type = parts[0].trim().toLowerCase();
            String title = parts[1].trim();

            float chartHeight = 250;
            if (yPos + chartHeight + paints.getChartTitlePaint().getTextSize() > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                Object[] result = finishAndStartNewPage(document, page, currentPageNum);
                page = (PdfDocument.Page) result[0];
                currentPageNum = (int) result[1];
                canvas = page.getCanvas();
                yPos = PaintManager.MARGIN;
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
                default:
                    throw new IllegalArgumentException("Unsupported chart type: " + type);
            }

            yPos += chartHeight + PaintManager.VISUAL_BOTTOM_MARGIN;

        } catch (Exception e) {
            Log.e("ContentDrawer", "Failed to parse or draw chart: " + chartString, e);
            Paint errorPaint = new Paint(paints.getTextPaint());
            errorPaint.setColor(Color.RED);
            errorPaint.setTextAlign(Paint.Align.CENTER);
            String errorMsg = "Error: Could not render chart. Check data format.";
            canvas.drawText(errorMsg, PaintManager.PAGE_WIDTH / 2f, yPos + 20, errorPaint);
            yPos += 40;
        }
        return new Object[]{page, yPos, currentPageNum};
    }

    private void drawBarChart(Canvas canvas, String[] parts, float yPos) {
        List<String> labels = parseStringList(parts[2]);
        List<Float> values = parseFloatList(parts[3]);
        if (labels.size() != values.size() || values.isEmpty()) {
            throw new IllegalArgumentException("Bar chart data mismatch or is empty.");
        }

        float chartHeight = 180; float chartWidth = PaintManager.CONTENT_WIDTH - 40;
        float left = PaintManager.MARGIN + 30; float bottom = yPos + chartHeight;
        canvas.drawLine(left, yPos, left, bottom, paints.getChartAxisPaint());
        canvas.drawLine(left, bottom, left + chartWidth, bottom, paints.getChartAxisPaint());
        float maxValue = Collections.max(values);
        if (maxValue == 0) maxValue = 1;

        float barWidth = (chartWidth / values.size()) * 0.6f;
        float barSpacing = (chartWidth / values.size()) * 0.4f;
        Paint barPaint = new Paint(); barPaint.setStyle(Paint.Style.FILL);
        float currentX = left + barSpacing / 2;
        for (int i = 0; i < values.size(); i++) {
            float barHeight = (values.get(i) / maxValue) * chartHeight;
            barPaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawRect(currentX, bottom - barHeight, currentX + barWidth, bottom, barPaint);
            canvas.drawText(labels.get(i), currentX + barWidth / 2, bottom + 15, paints.getChartLabelPaint());
            currentX += barWidth + barSpacing;
        }
    }

    private void drawPieChart(Canvas canvas, String[] parts, float yPos) {
        List<String> labels = parseStringList(parts[2]);
        List<Float> values = parseFloatList(parts[3]);
        if (labels.size() != values.size() || values.isEmpty()) {
            throw new IllegalArgumentException("Pie chart data mismatch or is empty.");
        }

        float total = 0; for (float v : values) total += v;
        if (total == 0) return;

        float chartSize = 150; float legendWidth = 120;
        float left = PaintManager.MARGIN + (PaintManager.CONTENT_WIDTH - chartSize - legendWidth) / 2;
        RectF oval = new RectF(left, yPos, left + chartSize, yPos + chartSize);
        Paint slicePaint = new Paint(); slicePaint.setStyle(Paint.Style.FILL); slicePaint.setAntiAlias(true);
        float startAngle = -90;
        for (int i = 0; i < values.size(); i++) {
            float sweepAngle = (values.get(i) / total) * 360;
            slicePaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawArc(oval, startAngle, sweepAngle, true, slicePaint);
            startAngle += sweepAngle;
        }
        float legendX = left + chartSize + 20; float legendY = yPos + 10;
        Paint legendPaint = new Paint(paints.getChartLabelPaint()); legendPaint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < labels.size(); i++) {
            slicePaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawRect(legendX, legendY, legendX + 10, legendY + 10, slicePaint);
            canvas.drawText(labels.get(i) + String.format(" (%.0f)", values.get(i)), legendX + 15, legendY + 9, legendPaint);
            legendY += 20;
        }
    }

    private void drawLineChart(Canvas canvas, String[] parts, float yPos) {
        if (parts.length < 6) throw new IllegalArgumentException("Line chart requires 6 parts.");
        List<Float> xValues = parseFloatList(parts[4]);
        List<Float> yValues = parseFloatList(parts[5]);
        if (xValues.size() != yValues.size() || xValues.isEmpty()) {
            throw new IllegalArgumentException("Line chart data mismatch or is empty.");
        }

        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 60;
        float left = PaintManager.MARGIN + 40;

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
        float bottom = yPos + chartHeight;

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
        if (parts.length < 5) throw new IllegalArgumentException("Scatter plot requires at least 5 parts.");
        List<Float> xValues = new ArrayList<>();
        List<Float> yValues = new ArrayList<>();
        for (int i = 4; i < parts.length; i++) {
            String[] point = parts[i].split(",");
            if (point.length == 2) {
                try {
                    xValues.add(Float.parseFloat(point[0].trim()));
                    yValues.add(Float.parseFloat(point[1].trim()));
                } catch (NumberFormatException e) {
                    Log.w("ContentDrawer", "Skipping invalid point in scatter plot: " + parts[i]);
                }
            }
        }
        if (xValues.isEmpty()) return;

        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 60;
        float left = PaintManager.MARGIN + 40;

        drawChartAxesAndGrid(canvas, left, yPos, chartWidth, chartHeight, xValues, yValues, parts[2].split(","));

        Paint pointPaint = new Paint();
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        float minX = Collections.min(xValues); float maxX = Collections.max(xValues);
        float minY = Collections.min(yValues); float maxY = Collections.max(yValues);
        if (maxX == minX) maxX += 1;
        if (maxY == minY) maxY += 1;
        float bottom = yPos + chartHeight;

        for (int i = 0; i < xValues.size(); i++) {
            float px = left + ((xValues.get(i) - minX) / (maxX - minX)) * chartWidth;
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            pointPaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawCircle(px, py, 5f, pointPaint);
        }
    }

    /**
     * FIX: This method has been completely rewritten to correctly render a combined
     * bar and line chart, including support for a second Y-axis to handle
     * different data scales.
     */
    private void drawCombinedChart(Canvas canvas, String[] parts, float yPos) {
        if (parts.length < 7) throw new IllegalArgumentException("Combined chart requires 7 parts.");

        List<String> categories = parseStringList(parts[4]);
        List<Float> barValues = parseFloatList(parts[5]);
        List<Float> lineValues = parseFloatList(parts[6]);

        if (categories.size() != barValues.size() || categories.size() != lineValues.size() || categories.isEmpty()) {
            throw new IllegalArgumentException("Combined chart data mismatch or is empty.");
        }

        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 80; // Space for two Y-axes
        float left = PaintManager.MARGIN + 40;
        float bottom = yPos + chartHeight;
        float right = left + chartWidth;

        // --- Draw Bar Component ---
        float maxBarValue = barValues.isEmpty() ? 1f : Collections.max(barValues);
        if (maxBarValue == 0) maxBarValue = 1;

        float barWidth = (chartWidth / barValues.size()) * 0.6f;
        float barSpacing = (chartWidth / barValues.size()) * 0.4f;
        Paint barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);
        float currentX = left + barSpacing / 2;

        paints.getChartLabelPaint().setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < barValues.size(); i++) {
            float barHeight = (barValues.get(i) / maxBarValue) * chartHeight;
            barPaint.setColor(paints.getChartColors()[0]); // Bar color
            canvas.drawRect(currentX, bottom - barHeight, currentX + barWidth, bottom, barPaint);
            canvas.drawText(categories.get(i), currentX + barWidth / 2, bottom + 15, paints.getChartLabelPaint());
            currentX += barWidth + barSpacing;
        }

        // --- Draw Line Component ---
        float maxLineValue = lineValues.isEmpty() ? 1f : Collections.max(lineValues);
        float minLineValue = lineValues.isEmpty() ? 0f : Collections.min(lineValues);
        if (maxLineValue == minLineValue) maxLineValue += 1;

        Path linePath = new Path();
        Paint linePaint = new Paint();
        linePaint.setColor(paints.getChartColors()[1]); // Line color
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);
        Paint pointPaint = new Paint(linePaint);
        pointPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < lineValues.size(); i++) {
            float px = left + (barSpacing / 2) + (i * (barWidth + barSpacing)) + (barWidth / 2);
            float py = bottom - ((lineValues.get(i) - minLineValue) / (maxLineValue - minLineValue)) * chartHeight;
            if (i == 0) {
                linePath.moveTo(px, py);
            } else {
                linePath.lineTo(px, py);
            }
            canvas.drawCircle(px, py, 4f, pointPaint);
        }
        canvas.drawPath(linePath, linePaint);

        // --- Draw Axes ---
        // Left Y-Axis (for Bars)
        paints.getChartLabelPaint().setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 5; i++) {
            float value = (i * maxBarValue / 5);
            float y = bottom - (i * chartHeight / 5);
            canvas.drawText(String.format("%.0f", value), left - 5, y + 3, paints.getChartLabelPaint());
            canvas.drawLine(left, y, right, y, paints.getChartGridPaint());
        }
        canvas.drawLine(left, yPos, left, bottom, paints.getChartAxisPaint());

        // Right Y-Axis (for Line)
        paints.getChartLabelPaint().setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 5; i++) {
            float value = minLineValue + (i * (maxLineValue - minLineValue) / 5);
            float y = bottom - (i * chartHeight / 5);
            canvas.drawText(String.format("%.1f", value), right + 5, y + 3, paints.getChartLabelPaint());
        }
        canvas.drawLine(right, yPos, right, bottom, paints.getChartAxisPaint());

        // X-Axis
        canvas.drawLine(left, bottom, right, bottom, paints.getChartAxisPaint());
    }

    private void drawChartAxesAndGrid(Canvas canvas, float left, float top, float width, float height, List<Float> xData, List<Float> yData, String[] axisLabels) {
        float bottom = top + height;
        float right = left + width;

        canvas.drawLine(left, top, left, bottom, paints.getChartAxisPaint());
        canvas.drawLine(left, bottom, right, bottom, paints.getChartAxisPaint());

        float minY = yData.isEmpty() ? 0f : Collections.min(yData);
        float maxY = yData.isEmpty() ? 1f : Collections.max(yData);
        if (maxY == minY) maxY +=1;
        int numGridLinesY = 5;
        paints.getChartLabelPaint().setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= numGridLinesY; i++) {
            float value = minY + (i * (maxY - minY) / numGridLinesY);
            float yPos = bottom - (i * height / numGridLinesY);
            canvas.drawText(String.format("%.1f", value), left - 5, yPos + 3, paints.getChartLabelPaint());
            canvas.drawLine(left, yPos, right, yPos, paints.getChartGridPaint());
        }

        float minX = xData.isEmpty() ? 0f : Collections.min(xData);
        float maxX = xData.isEmpty() ? 1f : Collections.max(xData);
        if (maxX == minX) maxX +=1;
        int numGridLinesX = Math.min(xData.size() - 1, 5);
        if (numGridLinesX <= 0) numGridLinesX = 1;
        paints.getChartLabelPaint().setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i <= numGridLinesX; i++) {
            float value = minX + (i * (maxX - minX) / numGridLinesX);
            float xPos = left + (i * width / numGridLinesX);
            canvas.drawText(String.format("%.1f", value), xPos, bottom + 15, paints.getChartLabelPaint());
        }

        if (axisLabels.length > 0) {
            canvas.drawText(axisLabels[0], left + width / 2, bottom + 30, paints.getChartLabelPaint());
            if (axisLabels.length > 1) {
                canvas.save();
                canvas.rotate(-90, left - 30, top + height / 2);
                canvas.drawText(axisLabels[1], left - 30, top + height / 2, paints.getChartLabelPaint());
                canvas.restore();
            }
        }
    }

    private Object[] drawTable(PdfDocument document, PdfDocument.Page page, String tableString, float yPos, int currentPageNum) {
        Canvas canvas = page.getCanvas();
        try {
            tableString = tableString.replace("[[TABLE|", "").replace("]]", "");
            String[] parts = tableString.split("\\|");
            if (parts.length < 3) throw new IllegalArgumentException("Invalid table format: Not enough parts.");

            String title = parts[0].trim();
            String[] headers = parts[1].split(",");
            List<String[]> rows = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                rows.add(parts[i].split(","));
            }

            if (yPos + 60 > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                Object[] result = finishAndStartNewPage(document, page, currentPageNum);
                page = (PdfDocument.Page) result[0];
                currentPageNum = (int) result[1];
                canvas = page.getCanvas();
                yPos = PaintManager.MARGIN;
            }
            yPos += PaintManager.VISUAL_TITLE_MARGIN;
            canvas.drawText(title, PaintManager.PAGE_WIDTH / 2f, yPos - paints.getChartTitlePaint().ascent(), paints.getChartTitlePaint());
            yPos += (paints.getChartTitlePaint().descent() - paints.getChartTitlePaint().ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;

            float tableWidth = PaintManager.CONTENT_WIDTH;
            float left = PaintManager.MARGIN;
            float[] colWidths = calculateColumnWidths(headers, rows, tableWidth);

            Object[] headerResult = drawRow(document, page, canvas, headers, yPos, currentPageNum, colWidths, left, true, headers);
            page = (PdfDocument.Page) headerResult[0];
            yPos = (float) headerResult[1];
            currentPageNum = (int) headerResult[2];
            canvas = page.getCanvas();

            for (String[] rowData : rows) {
                Object[] rowResult = drawRow(document, page, canvas, rowData, yPos, currentPageNum, colWidths, left, false, headers);
                page = (PdfDocument.Page) rowResult[0];
                yPos = (float) rowResult[1];
                currentPageNum = (int) rowResult[2];
                canvas = page.getCanvas();
            }

            yPos += PaintManager.VISUAL_BOTTOM_MARGIN;

        } catch (Exception e) {
            Log.e("ContentDrawer", "Failed to parse or draw table: " + tableString, e);
            Paint errorPaint = new Paint(paints.getTextPaint());
            errorPaint.setColor(Color.RED);
            errorPaint.setTextAlign(Paint.Align.CENTER);
            String errorMsg = "Error: Could not render table. Check data format.";
            canvas.drawText(errorMsg, PaintManager.PAGE_WIDTH / 2f, yPos + 20, errorPaint);
            yPos += 40;
        }

        return new Object[]{page, yPos, currentPageNum};
    }

    private float[] calculateColumnWidths(String[] headers, List<String[]> rows, float tableWidth) {
        float[] colWidths = new float[headers.length];
        // For now, simple equal distribution. A more complex implementation could measure text.
        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = tableWidth / headers.length;
        }
        return colWidths;
    }

    private Object[] drawRow(PdfDocument document, PdfDocument.Page page, Canvas canvas, String[] rowData, float yPos, int currentPageNum, float[] colWidths, float left, boolean isHeader, String[] headers) {
        Paint textPaint = isHeader ? paints.getTableHeaderPaint() : paints.getTableCellPaint();
        float cellPadding = PaintManager.CELL_PADDING;
        float maxTextHeightInRow = 0;
        List<List<String>> wrappedLinesByColumn = new ArrayList<>();
        int numColumns = headers.length;

        for (int i = 0; i < numColumns; i++) {
            float colWidth = colWidths[i] - (2 * cellPadding);
            String cellData = (i < rowData.length) ? rowData[i].trim() : "";
            List<String> wrappedLines = splitTextIntoLines(cellData, textPaint, colWidth);
            wrappedLinesByColumn.add(wrappedLines);
            float textHeight = wrappedLines.size() * (textPaint.descent() - textPaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
            if (textHeight > maxTextHeightInRow) {
                maxTextHeightInRow = textHeight;
            }
        }

        float rowHeight = maxTextHeightInRow + (2 * cellPadding);

        if (yPos + rowHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
            Object[] result = finishAndStartNewPage(document, page, currentPageNum);
            page = (PdfDocument.Page) result[0];
            currentPageNum = (int) result[1];
            canvas = page.getCanvas();
            yPos = PaintManager.MARGIN;
            if (!isHeader) {
                Object[] headerResult = drawRow(document, page, canvas, headers, yPos, currentPageNum, colWidths, left, true, headers);
                page = (PdfDocument.Page) headerResult[0];
                yPos = (float) headerResult[1];
                currentPageNum = (int) headerResult[2];
                canvas = page.getCanvas();
            }
        }

        float currentX = left;
        for (int i = 0; i < numColumns; i++) {
            float colWidth = colWidths[i];
            if (isHeader) {
                canvas.drawRect(currentX, yPos, currentX + colWidth, yPos + rowHeight, paints.getTableHeaderBgPaint());
            }

            float textY = yPos + cellPadding - textPaint.ascent();
            if (i < wrappedLinesByColumn.size()) {
                for (String line : wrappedLinesByColumn.get(i)) {
                    canvas.drawText(line, currentX + cellPadding, textY, textPaint);
                    textY += (textPaint.descent() - textPaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
                }
            }
            currentX += colWidth;
        }

        canvas.drawLine(left, yPos, left + PaintManager.CONTENT_WIDTH, yPos, paints.getTableBorderPaint());
        canvas.drawLine(left, yPos + rowHeight, left + PaintManager.CONTENT_WIDTH, yPos + rowHeight, paints.getTableBorderPaint());
        currentX = left;
        for (int i = 0; i < numColumns; i++) {
            canvas.drawLine(currentX, yPos, currentX, yPos + rowHeight, paints.getTableBorderPaint());
            currentX += colWidths[i];
        }
        canvas.drawLine(left + PaintManager.CONTENT_WIDTH, yPos, left + PaintManager.CONTENT_WIDTH, yPos + rowHeight, paints.getTableBorderPaint());

        return new Object[]{page, yPos + rowHeight, currentPageNum};
    }

    public void drawPageNumber(Canvas canvas, int pageNum) {
        if (pageNum <= 2) {
            return;
        }
        paints.getPageNumberPaint().setTextAlign(Paint.Align.CENTER);
        int logicalPageNum = pageNum - 2;
        canvas.drawText(String.valueOf(logicalPageNum), PaintManager.PAGE_WIDTH / 2f, PaintManager.PAGE_HEIGHT - 20, paints.getPageNumberPaint());
    }

    private Object[] finishAndStartNewPage(PdfDocument document, PdfDocument.Page page, int currentPageNum) {
        drawPageNumber(page.getCanvas(), currentPageNum);
        document.finishPage(page);

        int newPageNumber = currentPageNum + 1;
        PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, newPageNumber).create();
        PdfDocument.Page newPage = document.startPage(newPageInfo);
        return new Object[]{newPage, newPageNumber};
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
        if (text == null || text.isEmpty() || maxWidth <= 0) return lines;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String potentialLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (paint.measureText(potentialLine) < maxWidth) {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
                while (paint.measureText(currentLine.toString()) > maxWidth) {
                    int breakPoint = paint.breakText(currentLine.toString(), true, maxWidth, null);
                    if (breakPoint <= 0) { // Should not happen, but as a safeguard
                        lines.add(currentLine.substring(0, 1));
                        currentLine = new StringBuilder(currentLine.substring(1));
                    } else {
                        lines.add(currentLine.substring(0, breakPoint));
                        currentLine = new StringBuilder(currentLine.substring(breakPoint));
                    }
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
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

    /**
     * NEW: Helper method for robustly parsing a comma-separated string into a list of strings.
     */
    private List<String> parseStringList(String commaSeparatedString) {
        List<String> list = new ArrayList<>();
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) return list;
        for (String s : commaSeparatedString.split(",")) {
            if (!s.trim().isEmpty()) {
                list.add(s.trim());
            }
        }
        return list;
    }

    /**
     * NEW: Helper method for robustly parsing a comma-separated string into a list of floats.
     */
    private List<Float> parseFloatList(String commaSeparatedString) {
        List<Float> list = new ArrayList<>();
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) return list;
        for (String s : commaSeparatedString.split(",")) {
            if (!s.trim().isEmpty()) {
                try {
                    list.add(Float.parseFloat(s.trim()));
                } catch (NumberFormatException e) {
                    Log.w("ContentDrawer", "Skipping invalid number in chart data: '" + s + "'");
                }
            }
        }
        return list;
    }
}
