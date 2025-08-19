package com.pdf.ai.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

public class MarkdownParser {
    private static final Parser PARSER = Parser.builder().build();

    // Normalize incoming markdown from LLMs: strip code fences, normalize line-endings, collapse blank lines
    public static String normalize(String markdown) {
        if (markdown == null) return "";
        String s = markdown.trim();
        if (s.startsWith("```markdown")) {
            s = s.substring(11).trim();
        } else if (s.startsWith("```")) {
            s = s.substring(3).trim();
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3).trim();
        }
        // Normalize line endings
        s = s.replace("\r\n", "\n").replace('\r', '\n');
        // Collapse 3+ blank lines to at most 2
        s = s.replaceAll("\n{3,}", "\n\n");
        return s;
    }

    // Expose parser for future structured handling (e.g., robust lists/tables rendering)
    public static Node parse(String markdown) {
        String s = normalize(markdown);
        return PARSER.parse(s);
    }
}
