package com.pdf.ai.provider;

public class ProviderFactory {

    public static LLMProvider create(String providerName, String model, String geminiApiKey) {
        if ("Gemini".equals(providerName)) {
            if (geminiApiKey == null || geminiApiKey.isEmpty()) return null;
            return new GeminiProvider(geminiApiKey, model);
        }
        if ("GPT-OSS".equals(providerName)) {
            return new GptOssProvider(model);
        }
        return null;
    }
}
