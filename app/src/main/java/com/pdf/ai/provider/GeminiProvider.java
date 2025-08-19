package com.pdf.ai.provider;

import com.pdf.ai.GeminiApiClient;
import com.pdf.ai.OutlineData;
import com.pdf.ai.parser.OutlineParser;

import org.json.JSONException;
import org.json.JSONObject;

public class GeminiProvider implements LLMProvider {

    private final GeminiApiClient client;
    private final String apiKey;
    private final String model;

    public GeminiProvider(String apiKey, String model) {
        this.client = new GeminiApiClient();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public void generateOutline(String userPrompt, OutlineCallback callback) {
        client.generateOutline(userPrompt, apiKey, model, new GeminiApiClient.GeminiApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String outlineText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    OutlineData outlineData = OutlineParser.parseFromJsonText(outlineText);
                    callback.onSuccess(outlineData);
                } catch (JSONException e) {
                    callback.onFailure("Failed to parse outline: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    @Override
    public void generateSectionContent(String pdfTitle, String sectionTitle, java.util.List<String> allSections, int currentSectionIndex, SectionCallback callback) {
        client.generateSectionContent(pdfTitle, sectionTitle, allSections, currentSectionIndex, apiKey, model, new GeminiApiClient.GeminiApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String sectionContent = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                    callback.onSuccess(sectionContent);
                } catch (JSONException e) {
                    callback.onFailure("Failed to parse section content: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }
}
