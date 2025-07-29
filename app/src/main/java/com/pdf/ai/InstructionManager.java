package com.pdf.ai;

import android.content.Context;
import android.util.Log;

/**
 * InstructionManager provides consistent instructions for generating interactive outlines
 * and enhanced responses across all AI models (Qwen, Gemini, etc.)
 */
public class InstructionManager {
    
    private static final String TAG = "InstructionManager";
    private final Context context;
    
    public InstructionManager(Context context) {
        this.context = context;
    }
    
    /**
     * Get outline generation instructions for any AI model
     */
    public String getOutlineGenerationInstructions(String userPrompt, boolean isQwenModel) {
        StringBuilder instructions = new StringBuilder();
        
        if (isQwenModel) {
            // Enhanced instructions for Qwen models
            instructions.append("Generate a comprehensive and detailed outline for a PDF document based on the following topic. ");
            instructions.append("Your response should include two parts:\n\n");
            instructions.append("1. INTERACTIVE OUTLINE: Provide a structured outline in JSON format with a 'title' field for the main PDF title ");
            instructions.append("and a 'sections' array. Each section should have: 'section_title', 'description' (brief summary), ");
            instructions.append("'estimated_pages' (1-5), and 'key_points' (array of 3-5 main points to cover).\n\n");
            instructions.append("2. CONTENT PREVIEW: Provide a brief preview of what each section will contain.\n\n");
            instructions.append("JSON format example:\n");
            instructions.append("{\n");
            instructions.append("  \"title\": \"Comprehensive Guide to [Topic]\",\n");
            instructions.append("  \"sections\": [\n");
            instructions.append("    {\n");
            instructions.append("      \"section_title\": \"Introduction\",\n");
            instructions.append("      \"description\": \"Overview and background\",\n");
            instructions.append("      \"estimated_pages\": 2,\n");
            instructions.append("      \"key_points\": [\"Definition\", \"Importance\", \"Scope\"]\n");
            instructions.append("    }\n");
            instructions.append("  ]\n");
            instructions.append("}\n\n");
        } else {
            // Original Gemini instructions
            instructions.append("Generate a detailed outline for a PDF document based on the following topic. ");
            instructions.append("Provide the outline in a JSON format with a 'title' field for the main PDF title ");
            instructions.append("and a 'sections' array, where each object in the array has a 'section_title' field. ");
            instructions.append("Ensure the JSON is perfectly valid and contains no extra text or markdown outside the JSON object. ");
            instructions.append("Example format: {\"title\": \"My PDF Title\", \"sections\": [{\"section_title\": \"Introduction\"}, {\"section_title\": \"Body\"}, {\"section_title\": \"Conclusion\"}]}. ");
        }
        
        instructions.append("Topic: ").append(userPrompt);
        
        return instructions.toString();
    }
    
    /**
     * Get section content generation instructions for any AI model
     */
    public String getSectionContentInstructions(String pdfTitle, String sectionTitle, 
                                               java.util.List<String> allSections, 
                                               int currentSectionIndex, boolean isQwenModel) {
        StringBuilder instructions = new StringBuilder();
        
        if (isQwenModel) {
            // Enhanced instructions for Qwen models
            instructions.append("Generate comprehensive, professional, and engaging content for a PDF document section. ");
            instructions.append("Use your advanced reasoning capabilities to create high-quality, well-structured content.\n\n");
            instructions.append("DOCUMENT CONTEXT:\n");
            instructions.append("- PDF Title: \"").append(pdfTitle).append("\"\n");
            instructions.append("- Current Section: \"").append(sectionTitle).append("\"\n");
            instructions.append("- Section ").append(currentSectionIndex + 1).append(" of ").append(allSections.size()).append("\n\n");
            
            instructions.append("COMPLETE OUTLINE:\n");
            for (int i = 0; i < allSections.size(); i++) {
                instructions.append(i + 1).append(". ").append(allSections.get(i));
                if (i == currentSectionIndex) {
                    instructions.append(" ← CURRENT SECTION");
                }
                instructions.append("\n");
            }
            
            instructions.append("\nCONTENT REQUIREMENTS:\n");
            instructions.append("1. Write ONLY the content for \"").append(sectionTitle).append("\"\n");
            instructions.append("2. Use professional Markdown formatting (headings, lists, bold, italic)\n");
            instructions.append("3. Ensure logical flow and coherence with other sections\n");
            instructions.append("4. Include relevant examples, explanations, and insights\n");
            instructions.append("5. Aim for comprehensive coverage while maintaining readability\n");
            instructions.append("6. Do NOT wrap response in code blocks\n\n");
            
        } else {
            // Original Gemini instructions
            instructions.append("Generate detailed and professionally formatted content for a PDF document. ");
            instructions.append("The overall PDF title is: \"").append(pdfTitle).append("\". ");
            instructions.append("You are currently writing the section titled: \"").append(sectionTitle).append("\". ");
            instructions.append("The complete outline of the PDF is: ");
            for (int i = 0; i < allSections.size(); i++) {
                instructions.append(i + 1).append(". ").append(allSections.get(i));
                if (i == currentSectionIndex) {
                    instructions.append(" (Current Section)");
                }
                instructions.append("\n");
            }
            instructions.append("Provide the content for the section \"").append(sectionTitle).append("\" only. ");
            instructions.append("Format the content using standard Markdown. This includes headings (#, ##), lists (* or 1.), bold (**text**), and italic (*text*). Do not wrap the response in ```markdown blocks.");
        }
        
        // Enhanced chart and table instructions for both models
        instructions.append("\n\nDATA VISUALIZATION OPTIONS:\n");
        instructions.append("You can include data visualizations when they significantly enhance understanding. Use these formats ONLY when appropriate:\n");
        instructions.append("• Tables: [[TABLE|Table Title|Column1,Column2,Column3|Row1Val1,Row1Val2,Row1Val3|Row2Val1,Row2Val2,Row2Val3]]\n");
        instructions.append("• Bar Charts: [[CHART|bar|Chart Title|X-Label1,X-Label2|Value1,Value2]]\n");
        instructions.append("• Pie Charts: [[CHART|pie|Chart Title|Label1,Label2,Label3|Value1,Value2,Value3]]\n");
        instructions.append("• Line Charts: [[CHART|line|Chart Title|X-Axis,Y-Axis|X1,X2,X3|Y1,Y2,Y3]]\n");
        instructions.append("• Scatter Plots: [[CHART|scatter|Chart Title|X-Axis,Y-Axis|X1,Y1|X2,Y2|X3,Y3]]\n");
        instructions.append("Use visualizations sparingly and only when they add genuine value to the content.");
        
        return instructions.toString();
    }
    
    /**
     * Get enhanced general chat instructions for Qwen models
     */
    public String getGeneralChatInstructions(String userPrompt, boolean thinkingEnabled, boolean webSearchEnabled) {
        StringBuilder instructions = new StringBuilder();
        
        if (thinkingEnabled) {
            instructions.append("Think step by step about this request. Use your reasoning capabilities to provide a comprehensive and well-thought-out response. ");
        }
        
        if (webSearchEnabled) {
            instructions.append("If relevant and helpful, search for current information to supplement your response. ");
        }
        
        instructions.append("Provide a detailed, helpful, and engaging response. Use clear structure and formatting when appropriate. ");
        instructions.append("Focus on being informative, accurate, and user-friendly.\n\n");
        instructions.append("User request: ").append(userPrompt);
        
        return instructions.toString();
    }
    
    /**
     * Get model-specific optimization instructions
     */
    public String getModelOptimizationTips(String modelId) {
        switch (modelId) {
            case "qwen3-235b-a22b":
            case "qwen3-coder-plus":
            case "qwen3-32b":
                return "This is a powerful Qwen model with advanced reasoning capabilities. Utilize thinking mode for complex tasks and web search for current information.";
            case "qwen-max-latest":
            case "qwen-plus-2025-01-25":
                return "This Qwen model excels at complex reasoning and instruction following. Enable thinking mode for best results on analytical tasks.";
            case "gemini-1.5-pro-latest":
            case "gemini-1.5-flash-latest":
                return "This Gemini model works best with clear, structured prompts and specific instructions for desired output format.";
            default:
                return "Use clear, specific instructions and enable available features for optimal performance.";
        }
    }
}