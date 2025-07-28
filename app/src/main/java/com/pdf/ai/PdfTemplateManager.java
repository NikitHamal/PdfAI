package com.pdf.ai;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PdfTemplateManager {
    
    private static final String TAG = "PdfTemplateManager";
    private static final String TEMPLATES_KEY = "pdf_templates";
    private final PreferencesManager preferencesManager;
    
    public static class PdfTemplate {
        private String name;
        private String description;
        private String prompt;
        private List<String> sections;
        private String category;
        private boolean isDefault;
        
        public PdfTemplate(String name, String description, String prompt, List<String> sections, String category) {
            this.name = name;
            this.description = description;
            this.prompt = prompt;
            this.sections = sections;
            this.category = category;
            this.isDefault = false;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        
        public List<String> getSections() { return sections; }
        public void setSections(List<String> sections) { this.sections = sections; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
        
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("description", description);
            json.put("prompt", prompt);
            json.put("category", category);
            json.put("isDefault", isDefault);
            
            JSONArray sectionsArray = new JSONArray();
            for (String section : sections) {
                sectionsArray.put(section);
            }
            json.put("sections", sectionsArray);
            
            return json;
        }
        
        public static PdfTemplate fromJson(JSONObject json) throws JSONException {
            String name = json.getString("name");
            String description = json.getString("description");
            String prompt = json.getString("prompt");
            String category = json.getString("category");
            
            JSONArray sectionsArray = json.getJSONArray("sections");
            List<String> sections = new ArrayList<>();
            for (int i = 0; i < sectionsArray.length(); i++) {
                sections.add(sectionsArray.getString(i));
            }
            
            PdfTemplate template = new PdfTemplate(name, description, prompt, sections, category);
            template.setDefault(json.optBoolean("isDefault", false));
            
            return template;
        }
    }
    
    public PdfTemplateManager(Context context) {
        this.preferencesManager = new PreferencesManager(context);
    }
    
    public void saveTemplate(PdfTemplate template) {
        try {
            List<PdfTemplate> templates = loadTemplates();
            
            // Remove existing template with same name
            templates.removeIf(t -> t.getName().equals(template.getName()));
            
            // Add new template
            templates.add(template);
            
            saveTemplates(templates);
            Log.i(TAG, "Template saved: " + template.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving template", e);
        }
    }
    
    public void deleteTemplate(String templateName) {
        try {
            List<PdfTemplate> templates = loadTemplates();
            templates.removeIf(t -> t.getName().equals(templateName));
            saveTemplates(templates);
            Log.i(TAG, "Template deleted: " + templateName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting template", e);
        }
    }
    
    public List<PdfTemplate> loadTemplates() {
        List<PdfTemplate> templates = new ArrayList<>();
        
        try {
            String templatesJson = preferencesManager.getSharedPreferences().getString(TEMPLATES_KEY, null);
            if (templatesJson != null) {
                JSONArray jsonArray = new JSONArray(templatesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    templates.add(PdfTemplate.fromJson(jsonObject));
                }
            }
            
            // Add default templates if none exist
            if (templates.isEmpty()) {
                templates.addAll(getDefaultTemplates());
                saveTemplates(templates);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading templates", e);
        }
        
        return templates;
    }
    
    public List<PdfTemplate> getTemplatesByCategory(String category) {
        List<PdfTemplate> allTemplates = loadTemplates();
        List<PdfTemplate> categoryTemplates = new ArrayList<>();
        
        for (PdfTemplate template : allTemplates) {
            if (category.equals(template.getCategory())) {
                categoryTemplates.add(template);
            }
        }
        
        return categoryTemplates;
    }
    
    public PdfTemplate getTemplateByName(String name) {
        List<PdfTemplate> templates = loadTemplates();
        for (PdfTemplate template : templates) {
            if (name.equals(template.getName())) {
                return template;
            }
        }
        return null;
    }
    
    private void saveTemplates(List<PdfTemplate> templates) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (PdfTemplate template : templates) {
            jsonArray.put(template.toJson());
        }
        
        preferencesManager.getSharedPreferences().edit()
            .putString(TEMPLATES_KEY, jsonArray.toString())
            .apply();
    }
    
    private List<PdfTemplate> getDefaultTemplates() {
        List<PdfTemplate> defaultTemplates = new ArrayList<>();
        
        // Business Report Template
        List<String> businessSections = new ArrayList<>();
        businessSections.add("Executive Summary");
        businessSections.add("Introduction");
        businessSections.add("Market Analysis");
        businessSections.add("Financial Overview");
        businessSections.add("Recommendations");
        businessSections.add("Conclusion");
        
        PdfTemplate businessTemplate = new PdfTemplate(
            "Business Report",
            "Professional business report with executive summary and analysis",
            "Create a comprehensive business report with detailed analysis and professional formatting",
            businessSections,
            "Business"
        );
        businessTemplate.setDefault(true);
        defaultTemplates.add(businessTemplate);
        
        // Academic Paper Template
        List<String> academicSections = new ArrayList<>();
        academicSections.add("Abstract");
        academicSections.add("Introduction");
        academicSections.add("Literature Review");
        academicSections.add("Methodology");
        academicSections.add("Results");
        academicSections.add("Discussion");
        academicSections.add("Conclusion");
        academicSections.add("References");
        
        PdfTemplate academicTemplate = new PdfTemplate(
            "Academic Paper",
            "Standard academic paper format with proper sections",
            "Write an academic paper following standard research paper format",
            academicSections,
            "Academic"
        );
        academicTemplate.setDefault(true);
        defaultTemplates.add(academicTemplate);
        
        // Project Proposal Template
        List<String> proposalSections = new ArrayList<>();
        proposalSections.add("Project Overview");
        proposalSections.add("Objectives");
        proposalSections.add("Scope");
        proposalSections.add("Timeline");
        proposalSections.add("Budget");
        proposalSections.add("Risk Assessment");
        proposalSections.add("Conclusion");
        
        PdfTemplate proposalTemplate = new PdfTemplate(
            "Project Proposal",
            "Comprehensive project proposal with budget and timeline",
            "Create a detailed project proposal with clear objectives and implementation plan",
            proposalSections,
            "Project"
        );
        proposalTemplate.setDefault(true);
        defaultTemplates.add(proposalTemplate);
        
        return defaultTemplates;
    }
    
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        List<PdfTemplate> templates = loadTemplates();
        
        for (PdfTemplate template : templates) {
            if (!categories.contains(template.getCategory())) {
                categories.add(template.getCategory());
            }
        }
        
        return categories;
    }
}