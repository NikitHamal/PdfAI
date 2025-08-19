package com.pdf.ai.util;

import java.util.List;

public class PromptBuilder {
    public static String buildSectionPrompt(String pdfTitle, String sectionTitle, List<String> allSections, int currentSectionIndex) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Generate detailed and professionally formatted content for a PDF document. ");
        promptBuilder.append("The overall PDF title is: \"").append(pdfTitle).append("\". ");
        promptBuilder.append("You are currently writing the section titled: \"").append(sectionTitle).append("\". ");
        promptBuilder.append("The complete outline of the PDF is:\n");
        for (int i = 0; i < allSections.size(); i++) {
            promptBuilder.append(i + 1).append(". ").append(allSections.get(i));
            if (i == currentSectionIndex) {
                promptBuilder.append(" (Current Section)");
            }
            promptBuilder.append("\n");
        }
        promptBuilder.append("Provide the content for the section \"").append(sectionTitle).append("\" only. ");
        promptBuilder.append("Format the content using standard Markdown. This includes headings (#, ##), lists (* or 1.), bold (**text**), and italic (*text*). Do not wrap the response in code fences.");
        promptBuilder.append("\n\nIMPORTANT: You can also include data visualizations. If, and ONLY IF, a table or chart would significantly clarify the content, you can include it using one of the following special formats. Do NOT use them for decoration or for simple lists.\n");
        promptBuilder.append("For tables: [[TABLE|Table Title|Column1,Column2,Column3|Row1Val1,Row1Val2,Row1Val3|Row2Val1,Row2Val2,Row2Val3]]\n");
        promptBuilder.append("For bar charts: [[CHART|bar|Chart Title|X-Axis Label 1,X-Axis Label 2|Value1,Value2]]\n");
        promptBuilder.append("For pie charts: [[CHART|pie|Chart Title|Slice Label 1,Slice Label 2,Slice Label 3|Value1,Value2,Value3]]\n");
        promptBuilder.append("For line charts: [[CHART|line|Chart Title|X-Axis Label,Y-Axis Label|X-Val1,X-Val2,X-Val3|Y-Val1,Y-Val2,Y-Val3]]\n");
        promptBuilder.append("For scatter plots: [[CHART|scatter|Chart Title|X-Axis Label,Y-Axis Label|X1,Y1|X2,Y2|X3,Y3]]\n");
        promptBuilder.append("For combined bar-line charts: [[CHART|bar-line|Title|X-Axis Label,Y-Axis Label (Bar),Y-Axis Label (Line)|Cat1,Cat2|BarVal1,BarVal2|LineVal1,LineVal2]]\n");
        promptBuilder.append("Again, only use these special formats when they are the best way to present complex data. Otherwise, stick to standard Markdown text.");
        return promptBuilder.toString();
    }
}
