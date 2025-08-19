package com.pdf.ai.util;

public class MarkdownUtil {
    public static String clean(String markdownString) {
        if (markdownString == null) return "";
        String trimmed = markdownString.trim();
        if (trimmed.startsWith("```markdown")) {
            trimmed = trimmed.substring(11).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }
}
