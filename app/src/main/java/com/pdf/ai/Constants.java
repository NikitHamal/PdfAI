package com.pdf.ai;

public class Constants {
    // API Configuration
    public static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    public static final String DEFAULT_MODEL = "gemini-1.5-flash-latest";
    public static final int API_TIMEOUT_SECONDS = 60;
    
    // PDF Configuration
    public static final float DEFAULT_MARGIN = 72f;
    public static final float PAGE_HEIGHT = 792f;
    public static final float CONTENT_WIDTH = 648f;
    public static final float LINE_HEIGHT_MULTIPLIER = 1.2f;
    public static final float SECTION_TITLE_BOTTOM_MARGIN = 20f;
    public static final float HEADING_TOP_MARGIN = 15f;
    
    // UI Configuration
    public static final int MAX_MESSAGE_LINES = 5;
    public static final int MIN_EDIT_TEXT_HEIGHT = 48;
    public static final int PROGRESS_UPDATE_INTERVAL = 100;
    
    // File Configuration
    public static final String PDF_FILE_EXTENSION = ".pdf";
    public static final String CHAT_HISTORY_FILE = "chat_history.json";
    
    // Preferences Keys
    public static final String PREFS_NAME = "PdfAiPrefs";
    public static final String API_KEY_KEY = "geminiApiKey";
    public static final String CHAT_HISTORY_KEY = "chatHistory";
    public static final String SELECTED_MODEL_KEY = "selectedModel";
    public static final String DARK_MODE_KEY = "darkMode";
    public static final String AUTO_SAVE_KEY = "autoSave";
    
    // Error Messages
    public static final String ERROR_NO_API_KEY = "Please set your Gemini API key in settings";
    public static final String ERROR_NETWORK = "Network error. Please check your connection";
    public static final String ERROR_PDF_GENERATION = "Failed to generate PDF";
    public static final String ERROR_INVALID_RESPONSE = "Invalid response from AI service";
}