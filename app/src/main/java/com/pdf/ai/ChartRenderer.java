package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChartRenderer {
    private final PaintManager paints;

    public ChartRenderer(PaintManager paintManager) {
        this.paints = paintManager;
    }

    public Object[] drawChart(PdfDocument document, PdfDocument.Page page, String chartString, float yPos, int currentPageNum) {
        Canvas canvas = page.getCanvas();
        try {
            chartString = chartString.replace("[[CHART|", "").replace("]]", "");
            String[] parts = chartString.split("\\|");
            if (parts.length < 4) throw new IllegalArgumentException("Invalid chart format: Not enough parts.");

            String type = parts[0].trim().toLowerCase();
            String title = parts[1].trim();

            float chartHeight = 250;
            if (yPos + chartHeight + paints.getChartTitlePaint().getTextSize() > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
                Object[] result = PageHelper.finishAndStartNewPage(document, page, currentPageNum, paints);
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
            Log.e("ChartRenderer", "Failed to parse or draw chart: " + chartString, e);
            Paint errorPaint = new Paint(paints.getTextPaint());
            errorPaint.setColor(Color.RED);
            errorPaint.setTextAlign(Paint.Align.CENTER);
            String errorMsg = "Error: Could not render chart. Check data format.";
            page.getCanvas().drawText(errorMsg, PaintManager.PAGE_WIDTH / 2f, yPos + 20, errorPaint);
            yPos += 40;
        }
        return new Object[]{page, yPos, currentPageNum};
    }

    private List<String> parseStringList(String commaSeparatedString) {
        List<String> list = new ArrayList<>();
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) return list;
        for (String s : commaSeparatedString.split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }

    private List<Float> parseFloatList(String commaSeparatedString) {
        List<Float> list = new ArrayList<>();
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) return list;
        for (String s : commaSeparatedString.split(",")) {
            if (!s.trim().isEmpty()) {
                try { list.add(Float.parseFloat(s.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        return list;
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
        float total = 0; for (float v : values) total += v; if (total == 0) return;
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
        if (maxX == minX) maxX += 1; if (maxY == minY) maxY += 1;
        float bottom = yPos + chartHeight;
        for (int i = 0; i < xValues.size(); i++) {
            float px = left + ((xValues.get(i) - minX) / (maxX - minX)) * chartWidth;
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            if (i == 0) linePath.moveTo(px, py); else linePath.lineTo(px, py);
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
                try { xValues.add(Float.parseFloat(point[0].trim())); yValues.add(Float.parseFloat(point[1].trim())); } catch (NumberFormatException ignored) {}
            }
        }
        if (xValues.isEmpty()) return;
        float chartHeight = 180;
        float chartWidth = PaintManager.CONTENT_WIDTH - 60;
        float left = PaintManager.MARGIN + 40;
        drawChartAxesAndGrid(canvas, left, yPos, chartWidth, chartHeight, xValues, yValues, parts[2].split(","));
        Paint pointPaint = new Paint(); pointPaint.setStyle(Paint.Style.FILL); pointPaint.setAntiAlias(true);
        float minX = Collections.min(xValues); float maxX = Collections.max(xValues);
        float minY = Collections.min(yValues); float maxY = Collections.max(yValues);
        if (maxX == minX) maxX += 1; if (maxY == minY) maxY += 1;
        float bottom = yPos + chartHeight;
        for (int i = 0; i < xValues.size(); i++) {
            float px = left + ((xValues.get(i) - minX) / (maxX - minX)) * chartWidth;
            float py = bottom - ((yValues.get(i) - minY) / (maxY - minY)) * chartHeight;
            pointPaint.setColor(paints.getChartColors()[i % paints.getChartColors().length]);
            canvas.drawCircle(px, py, 5f, pointPaint);
        }
    }

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
        float maxBarValue = barValues.isEmpty() ? 1f : Collections.max(barValues); if (maxBarValue == 0) maxBarValue = 1;
        float barWidth = (chartWidth / barValues.size()) * 0.6f; float barSpacing = (chartWidth / barValues.size()) * 0.4f;
        Paint barPaint = new Paint(); barPaint.setStyle(Paint.Style.FILL);
        float currentX = left + barSpacing / 2;
        paints.getChartLabelPaint().setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < barValues.size(); i++) {
            float barHeight = (barValues.get(i) / maxBarValue) * chartHeight;
            barPaint.setColor(paints.getChartColors()[0]);
            canvas.drawRect(currentX, bottom - barHeight, currentX + barWidth, bottom, barPaint);
            canvas.drawText(categories.get(i), currentX + barWidth / 2, bottom + 15, paints.getChartLabelPaint());
            currentX += barWidth + barSpacing;
        }
        float maxLineValue = lineValues.isEmpty() ? 1f : Collections.max(lineValues);
        float minLineValue = lineValues.isEmpty() ? 0f : Collections.min(lineValues);
        if (maxLineValue == minLineValue) maxLineValue += 1;
        Path linePath = new Path(); Paint linePaint = new Paint(); linePaint.setColor(paints.getChartColors()[1]); linePaint.setStyle(Paint.Style.STROKE); linePaint.setStrokeWidth(2f); linePaint.setAntiAlias(true);
        Paint pointPaint = new Paint(linePaint); pointPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < lineValues.size(); i++) {
            float px = left + (barSpacing / 2) + (i * (barWidth + barSpacing)) + (barWidth / 2);
            float py = bottom - ((lineValues.get(i) - minLineValue) / (maxLineValue - minLineValue)) * chartHeight;
            if (i == 0) linePath.moveTo(px, py); else linePath.lineTo(px, py);
            canvas.drawCircle(px, py, 4f, pointPaint);
        }
        canvas.drawPath(linePath, linePaint);
        paints.getChartLabelPaint().setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 5; i++) {
            float value = (i * maxBarValue / 5);
            float y = bottom - (i * chartHeight / 5);
            canvas.drawText(String.format("%.0f", value), left - 5, y + 3, paints.getChartLabelPaint());
            canvas.drawLine(left, y, right, y, paints.getChartGridPaint());
        }
        canvas.drawLine(left, yPos, left, bottom, paints.getChartAxisPaint());
        paints.getChartLabelPaint().setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i <= 5; i++) {
            float value = minLineValue + (i * (maxLineValue - minLineValue) / 5);
            float y = bottom - (i * chartHeight / 5);
            canvas.drawText(String.format("%.1f", value), right + 5, y + 3, paints.getChartLabelPaint());
        }
        canvas.drawLine(right, yPos, right, bottom, paints.getChartAxisPaint());
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
}
