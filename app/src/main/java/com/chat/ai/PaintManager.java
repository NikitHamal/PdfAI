package com.pdf.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import androidx.core.content.res.ResourcesCompat;

public class PaintManager {

    // Page dimensions and margins
    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;
    public static final int MARGIN = 40;
    public static final int CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    // Vertical spacing constants
    public static final float LINE_HEIGHT_MULTIPLIER = 1.2f;
    public static final float PARAGRAPH_SPACING = 12f;
    public static final float LIST_ITEM_INDENT = 20f;
    public static final float SECTION_TITLE_BOTTOM_MARGIN = 18f;
    public static final float HEADING_TOP_MARGIN = 10f;
    public static final float VISUAL_TITLE_MARGIN = 10f;
    public static final float VISUAL_BOTTOM_MARGIN = 20f;
    public static final float CELL_PADDING = 8f;

    // Paints
    private final Paint titlePaint, sectionTitlePaint, textPaint, boldTextPaint, italicTextPaint;
    private final Paint h1Paint, h2Paint, h3Paint;
    private final Paint tableBorderPaint, tableHeaderPaint, tableCellPaint, tableHeaderBgPaint, tableAltRowPaint;
    private final Paint chartTitlePaint, chartAxisPaint, chartLabelPaint, chartGridPaint, dottedLinePaint;
    private final Paint pageNumberPaint, tocTitlePaint, tocTextPaint, tocNumberPaint;
    private final int[] chartColors;

    public PaintManager(Context context) {
        Typeface regularTypeface = ResourcesCompat.getFont(context, R.font.reg);
        Typeface mediumTypeface = ResourcesCompat.getFont(context, R.font.med);
        Typeface semiboldTypeface = ResourcesCompat.getFont(context, R.font.sem);

        // Cover Page and Section Title Paints
        titlePaint = new Paint();
        titlePaint.setTextSize(32f);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(semiboldTypeface);
        titlePaint.setAntiAlias(true);

        sectionTitlePaint = new Paint();
        sectionTitlePaint.setTextSize(22f);
        sectionTitlePaint.setColor(0xFF1E88E5); // A nice blue
        sectionTitlePaint.setTypeface(semiboldTypeface);
        sectionTitlePaint.setAntiAlias(true);

        // Heading Paints
        h1Paint = new Paint(sectionTitlePaint);
        h1Paint.setTextSize(18f);
        h1Paint.setColor(Color.BLACK);
        h2Paint = new Paint(h1Paint);
        h2Paint.setTextSize(16f);
        h3Paint = new Paint(h1Paint);
        h3Paint.setTextSize(14f);
        h3Paint.setTypeface(mediumTypeface);

        // Standard Text Paints
        textPaint = new Paint();
        textPaint.setTextSize(11f);
        textPaint.setColor(0xFF333333);
        textPaint.setTypeface(regularTypeface);
        textPaint.setAntiAlias(true);

        boldTextPaint = new Paint(textPaint);
        boldTextPaint.setTypeface(semiboldTypeface);

        italicTextPaint = new Paint(textPaint);
        italicTextPaint.setTypeface(Typeface.create(regularTypeface, Typeface.ITALIC));

        // Table Paints
        tableBorderPaint = new Paint();
        tableBorderPaint.setStyle(Paint.Style.STROKE);
        tableBorderPaint.setColor(0xFFCCCCCC);
        tableBorderPaint.setStrokeWidth(1f);

        tableHeaderBgPaint = new Paint();
        tableHeaderBgPaint.setStyle(Paint.Style.FILL);
        tableHeaderBgPaint.setColor(0xFFF0F0F0);

        tableAltRowPaint = new Paint();
        tableAltRowPaint.setStyle(Paint.Style.FILL);
        tableAltRowPaint.setColor(0xFFFAFAFA);

        tableHeaderPaint = new TextPaint(boldTextPaint);
        tableHeaderPaint.setTextSize(10f);
        tableHeaderPaint.setColor(Color.BLACK);

        tableCellPaint = new TextPaint(textPaint);
        tableCellPaint.setTextSize(10f);

        // Chart Paints
        chartTitlePaint = new Paint(h3Paint);
        chartTitlePaint.setTextAlign(Paint.Align.CENTER);
        chartTitlePaint.setTextSize(12f);

        chartAxisPaint = new Paint();
        chartAxisPaint.setStrokeWidth(1.5f);
        chartAxisPaint.setColor(Color.DKGRAY);

        chartGridPaint = new Paint();
        chartGridPaint.setStrokeWidth(0.5f);
        chartGridPaint.setColor(0xFFE0E0E0);
        chartGridPaint.setStyle(Paint.Style.STROKE);

        chartLabelPaint = new Paint(textPaint);
        chartLabelPaint.setTextSize(9f);
        chartLabelPaint.setTextAlign(Paint.Align.CENTER);

        chartColors = new int[]{
                Color.parseColor("#4285F4"), Color.parseColor("#DB4437"),
                Color.parseColor("#F4B400"), Color.parseColor("#0F9D58"),
                Color.parseColor("#AB47BC"), Color.parseColor("#00ACC1"),
                Color.parseColor("#FF7043"), Color.parseColor("#9E9D24")
        };

        // Page Number and TOC Paints
        pageNumberPaint = new Paint();
        pageNumberPaint.setTextSize(9f);
        pageNumberPaint.setColor(Color.GRAY);
        pageNumberPaint.setTextAlign(Paint.Align.CENTER);
        pageNumberPaint.setTypeface(regularTypeface);

        tocTitlePaint = new Paint(titlePaint);
        tocTitlePaint.setTextSize(24f);

        tocTextPaint = new Paint(textPaint);
        tocTextPaint.setTextSize(12f);
        tocTextPaint.setTextAlign(Paint.Align.LEFT);

        tocNumberPaint = new Paint(tocTextPaint);
        tocNumberPaint.setTextAlign(Paint.Align.RIGHT);
        tocNumberPaint.setTypeface(semiboldTypeface);

        dottedLinePaint = new Paint();
        dottedLinePaint.setColor(Color.LTGRAY);
        dottedLinePaint.setStyle(Paint.Style.STROKE);
        dottedLinePaint.setStrokeWidth(1f);
        dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0));
    }

    // Getters for all paints
    public Paint getTitlePaint() { return titlePaint; }
    public Paint getSectionTitlePaint() { return sectionTitlePaint; }
    public Paint getTextPaint() { return textPaint; }
    public Paint getBoldTextPaint() { return boldTextPaint; }
    public Paint getItalicTextPaint() { return italicTextPaint; }
    public Paint getH1Paint() { return h1Paint; }
    public Paint getH2Paint() { return h2Paint; }
    public Paint getH3Paint() { return h3Paint; }
    public Paint getTableBorderPaint() { return tableBorderPaint; }
    public Paint getTableHeaderPaint() { return tableHeaderPaint; }
    public Paint getTableCellPaint() { return tableCellPaint; }
    public Paint getTableHeaderBgPaint() { return tableHeaderBgPaint; }
    public Paint getTableAltRowPaint() { return tableAltRowPaint; }
    public Paint getChartTitlePaint() { return chartTitlePaint; }
    public Paint getChartAxisPaint() { return chartAxisPaint; }
    public Paint getChartLabelPaint() { return chartLabelPaint; }
    public Paint getChartGridPaint() { return chartGridPaint; }
    public int[] getChartColors() { return chartColors; }
    public Paint getPageNumberPaint() { return pageNumberPaint; }
    public Paint getTocTitlePaint() { return tocTitlePaint; }
    public Paint getTocTextPaint() { return tocTextPaint; }
    public Paint getTocNumberPaint() { return tocNumberPaint; }
    public Paint getDottedLinePaint() { return dottedLinePaint; }
}
