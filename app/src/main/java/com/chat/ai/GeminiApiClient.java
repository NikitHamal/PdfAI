package com.pdf.ai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final OkHttpClient client;

    public GeminiApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface GeminiApiCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public void generateOutline(String userPrompt, String apiKey, String model, GeminiApiCallback callback) {
        String prompt = "Generate a detailed outline for a PDF document based on the following topic. " +
                "Provide the outline in a JSON format with a 'title' field for the main PDF title " +
                "and a 'sections' array, where each object in the array has a 'section_title' field. " +
                "Ensure the JSON is perfectly valid and contains no extra text or markdown outside the JSON object. " +
                "Example format: {\"title\": \"My PDF Title\", \"sections\": [{\"section_title\": \"Introduction\"}, {\"section_title\": \"Body\"}, {\"section_title\": \"Conclusion\"}]}. " +
                "Topic: " + userPrompt;

        JSONObject requestBodyJson = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            JSONObject content = new JSONObject();
            content.put("parts", new JSONArray().put(part));
            contents.put(content);
            requestBodyJson.put("contents", contents);
        } catch (JSONException e) {
            Log.e("GeminiApiClient", "Error creating JSON request for outline", e);
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + model + ":generateContent?key=" + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure("Error: " + response.code() + " - " + errorBody);
                }
            }
        });
    }

    public void generateSectionContent(String pdfTitle, String sectionTitle, List<String> allSections, int currentSectionIndex, String apiKey, String model, GeminiApiCallback callback) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Generate detailed and professionally formatted content for a PDF document. ");
        promptBuilder.append("The overall PDF title is: \"").append(pdfTitle).append("\". ");
        promptBuilder.append("You are currently writing the section titled: \"").append(sectionTitle).append("\". ");
        promptBuilder.append("The complete outline of the PDF is: ");
        for (int i = 0; i < allSections.size(); i++) {
            promptBuilder.append(i + 1).append(". ").append(allSections.get(i));
            if (i == currentSectionIndex) {
                promptBuilder.append(" (Current Section)");
            }
            promptBuilder.append("\n");
        }
        promptBuilder.append("Provide the content for the section \"").append(sectionTitle).append("\" only. ");
        promptBuilder.append("Format the content using standard Markdown. This includes headings (#, ##), lists (* or 1.), bold (**text**), and italic (*text*). Do not wrap the response in ```markdown blocks.");

        // --- ENHANCED INSTRUCTION FOR CHARTS AND TABLES ---
        promptBuilder.append("\n\nIMPORTANT: You can also include data visualizations. If, and ONLY IF, a table or chart would significantly clarify the content, you can include it using one of the following special formats. Do NOT use them for decoration or for simple lists.\n");
        promptBuilder.append("For tables: [[TABLE|Table Title|Column1,Column2,Column3|Row1Val1,Row1Val2,Row1Val3|Row2Val1,Row2Val2,Row2Val3]]\n");
        promptBuilder.append("For bar charts: [[CHART|bar|Chart Title|X-Axis Label 1,X-Axis Label 2|Value1,Value2]]\n");
        promptBuilder.append("For pie charts: [[CHART|pie|Chart Title|Slice Label 1,Slice Label 2,Slice Label 3|Value1,Value2,Value3]]\n");
        promptBuilder.append("For line charts: [[CHART|line|Chart Title|X-Axis Label,Y-Axis Label|X-Val1,X-Val2,X-Val3|Y-Val1,Y-Val2,Y-Val3]]\n");
        promptBuilder.append("For scatter plots: [[CHART|scatter|Chart Title|X-Axis Label,Y-Axis Label|X1,Y1|X2,Y2|X3,Y3]]\n");
        promptBuilder.append("For combined bar-line charts: [[CHART|bar-line|Title|X-Axis Label,Y-Axis Label (Bar),Y-Axis Label (Line)|Cat1,Cat2|BarVal1,BarVal2|LineVal1,LineVal2]]\n");
        promptBuilder.append("Again, only use these special formats when they are the best way to present complex data. Otherwise, stick to standard Markdown text.");


        JSONObject requestBodyJson = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", promptBuilder.toString());
            JSONObject content = new JSONObject();
            content.put("parts", new JSONArray().put(part));
            contents.put(content);
            requestBodyJson.put("contents", contents);
        } catch (JSONException e) {
            Log.e("GeminiApiClient", "Error creating JSON request for section content", e);
            callback.onFailure("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + model + ":generateContent?key=" + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onFailure("Error: " + response.code() + " - " + errorBody);
                }
            }
        });
    }
}
