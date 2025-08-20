package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TableRenderer {
    private final PaintManager paints;

    public TableRenderer(PaintManager paintManager) {
        this.paints = paintManager;
    }

    public Object[] drawTable(PdfDocument document, PdfDocument.Page page, String tableString, float yPos, int currentPageNum) {
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
                Object[] result = PageHelper.finishAndStartNewPage(document, page, currentPageNum, paints);
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
            float[] colWidths = calculateColumnWidths(headers.length, tableWidth);

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
            Log.e("TableRenderer", "Failed to parse or draw table: " + tableString, e);
            Paint errorPaint = new Paint(paints.getTextPaint());
            errorPaint.setColor(Color.RED);
            errorPaint.setTextAlign(Paint.Align.CENTER);
            String errorMsg = "Error: Could not render table. Check data format.";
            canvas.drawText(errorMsg, PaintManager.PAGE_WIDTH / 2f, yPos + 20, errorPaint);
            yPos += 40;
        }
        return new Object[]{page, yPos, currentPageNum};
    }

    private float[] calculateColumnWidths(int numCols, float tableWidth) {
        float[] colWidths = new float[numCols];
        for (int i = 0; i < numCols; i++) {
            colWidths[i] = tableWidth / numCols;
        }
        return colWidths;
    }

    private Object[] drawRow(PdfDocument document, PdfDocument.Page page, Canvas canvas, String[] rowData, float yPos, int currentPageNum, float[] colWidths, float left, boolean isHeader, String[] headers) {
        Paint textPaint = isHeader ? paints.getTableHeaderPaint() : paints.getTableCellPaint();
        float cellPadding = PaintManager.CELL_PADDING;
        float maxTextHeightInRow = 0;
        int numColumns = headers.length;

        List<List<String>> wrappedLinesByColumn = new ArrayList<>();
        for (int i = 0; i < numColumns; i++) {
            float colWidth = colWidths[i] - (2 * cellPadding);
            String cellData = (i < rowData.length) ? rowData[i].trim() : "";
            List<String> wrappedLines = DrawUtils.splitTextIntoLines(cellData, textPaint, colWidth);
            wrappedLinesByColumn.add(wrappedLines);
            float textHeight = wrappedLines.size() * (textPaint.descent() - textPaint.ascent()) * PaintManager.LINE_HEIGHT_MULTIPLIER;
            if (textHeight > maxTextHeightInRow) {
                maxTextHeightInRow = textHeight;
            }
        }

        float rowHeight = maxTextHeightInRow + (2 * cellPadding);

        if (yPos + rowHeight > PaintManager.PAGE_HEIGHT - PaintManager.MARGIN) {
            Object[] result = PageHelper.finishAndStartNewPage(document, page, currentPageNum, paints);
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
}
