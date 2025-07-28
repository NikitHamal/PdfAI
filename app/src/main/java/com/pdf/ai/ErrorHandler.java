package com.pdf.ai;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    
    public static void handleApiError(Context context, String error, String operation) {
        Log.e(TAG, "API Error during " + operation + ": " + error);
        
        String userMessage;
        if (error.contains("401") || error.contains("403")) {
            userMessage = "Invalid API key. Please check your settings.";
        } else if (error.contains("429")) {
            userMessage = "Rate limit exceeded. Please try again later.";
        } else if (error.contains("500") || error.contains("502") || error.contains("503")) {
            userMessage = "Service temporarily unavailable. Please try again.";
        } else if (error.contains("timeout") || error.contains("Network")) {
            userMessage = "Network error. Please check your connection.";
        } else {
            userMessage = "An error occurred: " + error;
        }
        
        showToast(context, userMessage);
    }
    
    public static void handlePdfError(Context context, String error) {
        Log.e(TAG, "PDF Generation Error: " + error);
        
        String userMessage;
        if (error.contains("permission")) {
            userMessage = "Storage permission required to save PDF.";
        } else if (error.contains("memory")) {
            userMessage = "Insufficient memory to generate PDF.";
        } else if (error.contains("format")) {
            userMessage = "Invalid content format for PDF generation.";
        } else {
            userMessage = "Failed to generate PDF: " + error;
        }
        
        showToast(context, userMessage);
    }
    
    public static void handleFileError(Context context, String error) {
        Log.e(TAG, "File Operation Error: " + error);
        
        String userMessage;
        if (error.contains("permission")) {
            userMessage = "Storage permission required.";
        } else if (error.contains("not found")) {
            userMessage = "File not found.";
        } else if (error.contains("read only")) {
            userMessage = "File is read-only.";
        } else {
            userMessage = "File operation failed: " + error;
        }
        
        showToast(context, userMessage);
    }
    
    public static void handleValidationError(Context context, String field, String error) {
        Log.w(TAG, "Validation Error for " + field + ": " + error);
        showToast(context, "Please check " + field + ": " + error);
    }
    
    private static void showToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    public static void logError(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
    
    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
    }
    
    public static void logInfo(String tag, String message) {
        Log.i(tag, message);
    }
}