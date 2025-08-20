package com.pdf.ai;

import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class DrawUtils {
    private DrawUtils() {}

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
                    if (breakPoint <= 0) {
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
}
