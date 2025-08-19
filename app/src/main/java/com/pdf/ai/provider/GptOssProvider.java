package com.pdf.ai.provider;

import com.pdf.ai.GptOssClient;
import com.pdf.ai.OutlineData;
import com.pdf.ai.parser.OutlineParser;
import com.pdf.ai.util.PromptBuilder;

import org.json.JSONException;

import java.util.List;

public class GptOssProvider implements LLMProvider {

    private final GptOssClient client;
    private final String model;

    public GptOssProvider(String model) {
        this.client = new GptOssClient();
        this.model = model;
    }

    @Override
    public void generateOutline(String userPrompt, OutlineCallback callback) {
        String outlinePrompt = "Generate a detailed outline for a PDF document based on the following topic. " +
                "Provide ONLY a valid JSON object with a 'title' string and a 'sections' array of objects with 'section_title' strings. " +
                "Do not include any extra commentary or markdown. Topic: " + userPrompt;

        client.generateText(model, outlinePrompt, null, new GptOssClient.Callback() {
            @Override
            public void onSuccess(String text) {
                try {
                    OutlineData data = OutlineParser.parseFromJsonText(text);
                    callback.onSuccess(data);
                } catch (JSONException e) {
                    callback.onFailure("Failed to parse GPT-OSS outline: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("GPT-OSS outline failed: " + error);
            }
        });
    }

    @Override
    public void generateSectionContent(String pdfTitle, String sectionTitle, List<String> allSections, int currentSectionIndex, SectionCallback callback) {
        String sectionPrompt = PromptBuilder.buildSectionPrompt(pdfTitle, sectionTitle, allSections, currentSectionIndex);
        client.generateText(model, sectionPrompt, null, new GptOssClient.Callback() {
            @Override
            public void onSuccess(String text) {
                callback.onSuccess(text);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }
}
