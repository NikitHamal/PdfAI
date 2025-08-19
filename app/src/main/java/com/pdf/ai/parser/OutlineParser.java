package com.pdf.ai.parser;

import com.pdf.ai.OutlineData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OutlineParser {

    public static OutlineData parseFromJsonText(String jsonText) throws JSONException {
        String cleaned = cleanJson(jsonText);
        JSONObject outlineJson = new JSONObject(cleaned);
        String pdfTitle = outlineJson.getString("title");
        JSONArray sectionsArray = outlineJson.getJSONArray("sections");
        List<String> sections = new ArrayList<>();
        for (int i = 0; i < sectionsArray.length(); i++) {
            JSONObject obj = sectionsArray.getJSONObject(i);
            // support both array of strings or objects with section_title
            if (obj.has("section_title")) {
                sections.add(obj.getString("section_title"));
            } else if (obj.has("title")) {
                sections.add(obj.getString("title"));
            } else {
                // fallback: if array element is string
                sections.add(sectionsArray.getString(i));
            }
        }
        return new OutlineData(pdfTitle, sections);
    }

    public static String cleanJson(String jsonString) {
        if (jsonString == null) return "";
        String trimmed = jsonString.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
