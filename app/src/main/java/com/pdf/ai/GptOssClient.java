package com.pdf.ai;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * Minimal client for GPT-OSS free SSE API.
 * Endpoint: https://api.gpt-oss.com/chatkit
 * Headers: accept: text/event-stream, x-selected-model, x-reasoning-effort
 * Body (threads.create):
 * {
 *   "op": "threads.create",
 *   "params": { "input": { "text": <prompt>, "content": [{"type":"input_text","text":<prompt>}], "quoted_text": "", "attachments": [] } }
 * }
 *
 * Stream emits JSON objects per event. We accumulate text from entries where
 *   type == "assistant_message.content_part.text_delta" and field "delta" contains the text chunk.
 */
public class GptOssClient {

    private static final String TAG = "GptOssClient";
    private static final String API_ENDPOINT = "https://api.gpt-oss.com/chatkit";

    private final OkHttpClient client;

    public interface Callback {
        void onSuccess(String text);
        void onFailure(String error);
    }

    public GptOssClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                // SSE streams can be long-lived; disable read/call timeout
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(240, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void generateText(String model, String prompt, @Nullable String reasoningEffort, Callback callback) {
        try {
            JSONObject input = new JSONObject();
            input.put("text", prompt);
            // mirror provider payload structure
            input.put("content", new org.json.JSONArray()
                    .put(new JSONObject().put("type", "input_text").put("text", prompt)));
            input.put("quoted_text", "");
            input.put("attachments", new org.json.JSONArray());

            JSONObject params = new JSONObject();
            params.put("input", input);

            JSONObject body = new JSONObject();
            body.put("op", "threads.create");
            body.put("params", params);

            Request.Builder builder = new Request.Builder()
                    .url(API_ENDPOINT)
                    .addHeader("accept", "text/event-stream")
                    .addHeader("x-selected-model", model)
                    .addHeader("x-show-reasoning", "false");

            if (reasoningEffort != null && !reasoningEffort.isEmpty()) {
                builder.addHeader("x-reasoning-effort", reasoningEffort);
            }

            Request request = builder
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")))
                    .build();

            final StringBuilder aggregate = new StringBuilder();

            EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    // data is a JSON string per SSE event
                    try {
                        JSONObject json = new JSONObject(data);
                        String evtType = json.optString("type");
                        if ("thread.item_updated".equals(evtType)) {
                            JSONObject update = json.optJSONObject("update");
                            if (update == null) update = json.optJSONObject("entry");
                            if (update != null) {
                                String entryType = update.optString("type");
                                if ("assistant_message.content_part.text_delta".equals(entryType)) {
                                    String delta = update.optString("delta", "");
                                    aggregate.append(delta);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Malformed SSE event: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, @Nullable Response response) {
                    String error = t != null ? t.getMessage() : (response != null ? String.valueOf(response.code()) : "Unknown error");
                    callback.onFailure("GPT-OSS SSE error: " + error);
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    callback.onSuccess(aggregate.toString());
                }
            });
        } catch (JSONException e) {
            callback.onFailure("Failed to build request: " + e.getMessage());
        }
    }
}
