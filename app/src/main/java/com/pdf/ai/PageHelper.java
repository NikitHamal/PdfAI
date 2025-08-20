package com.pdf.ai;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

public final class PageHelper {
    private PageHelper() {}

    public static void drawPageNumber(Canvas canvas, int pageNum, PaintManager paints) {
        if (pageNum <= 2) return;
        Paint p = paints.getPageNumberPaint();
        p.setTextAlign(Paint.Align.CENTER);
        int logicalPageNum = pageNum - 2;
        canvas.drawText(String.valueOf(logicalPageNum), PaintManager.PAGE_WIDTH / 2f, PaintManager.PAGE_HEIGHT - 20, p);
    }

    public static Object[] finishAndStartNewPage(PdfDocument document, PdfDocument.Page page, int currentPageNum, PaintManager paints) {
        drawPageNumber(page.getCanvas(), currentPageNum, paints);
        document.finishPage(page);
        int newPageNumber = currentPageNum + 1;
        PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(PaintManager.PAGE_WIDTH, PaintManager.PAGE_HEIGHT, newPageNumber).create();
        PdfDocument.Page newPage = document.startPage(newPageInfo);
        return new Object[]{newPage, newPageNumber};
    }
}
