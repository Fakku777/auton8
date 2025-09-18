package com.april777.auton8.minescript;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.april777.auton8.core.Config;
import com.april777.auton8.core.JsonUtils;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MinescriptClient provides HTTP-based communication with n8n via Minescript API
 * Uses Server-Sent Events for real-time command reception and HTTP POST for event publishing
 */
public final class MinescriptClient {
    private final Config cfg;
    private final OkHttpClient httpClient;
    private EventSource eventSource;
    
    // endpoint -> handler (preserves MQTT-style topic handling)
    private final ConcurrentHashMap<String, MinescriptMessageHandler> handlers = new ConcurrentHashMap<>();
    
    private volatile boolean connected = false;
    private volatile boolean announcedOnce = false;
    
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public MinescriptClient(Config cfg) {
        this.cfg = cfg;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Register a handler for a specific endpoint (replaces MQTT topic subscription)
     */
    public void onMessage(String endpoint, MinescriptMessageHandler handler) {
        handlers.put(endpoint, handler);
    }
    
    public boolean isConnected() {
        return connected && eventSource != null;
    }
    
    /**
     * Connect to n8n via Minescript API and establish SSE stream for real-time events
     */
    public void connect() {
        try {
            if (connected && eventSource != null) return;
            
            // First, authenticate and get session token
            authenticate();
            
            // Establish SSE connection for real-time command reception
            String sseUrl = cfg.minescriptBaseUrl + "/events/stream";
            Request sseRequest = new Request.Builder()
                .url(sseUrl)
                .addHeader("Authorization", "Bearer " + cfg.authToken)
                .addHeader("X-Session-ID", cfg.sessionId)
                .build();
                
            eventSource = EventSources.createFactory(httpClient)
                .newEventSource(sseRequest, new EventSourceListener() {
                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        connected = true;
                        if (!announcedOnce) {
                            publishStatus("connected");
                            announcedOnce = true;
                        } else {
                            publishStatus("reconnected");
                        }
                    }
                    
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        handleIncomingEvent(type, data);
                    }
                    
                    @Override
                    public void onClosed(EventSource eventSource) {
                        connected = false;
                        publishStatus("disconnected");
                    }
                    
                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        connected = false;
                        publishStatus("connection_lost");
                        // Attempt reconnection after delay
                        scheduleReconnect();
                    }
                });
                
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Authenticate with n8n using custom auth credentials
     */
    private void authenticate() throws IOException {
        JsonObject authPayload = new JsonObject();
        authPayload.addProperty("client_id", cfg.clientId);
        authPayload.addProperty("auth_key", cfg.authKey);
        authPayload.addProperty("session_id", cfg.sessionId);
        
        RequestBody body = RequestBody.create(authPayload.toString(), JSON);
        Request request = new Request.Builder()
            .url(cfg.minescriptBaseUrl + "/auth")
            .post(body)
            .build();
            
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Authentication failed: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonObject authResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            cfg.authToken = authResponse.get("token").getAsString();
        }
    }
    
    /**
     * Handle incoming events from n8n (replaces MQTT message arrival)
     */
    private void handleIncomingEvent(String eventType, String data) {
        try {
            JsonObject eventData = JsonParser.parseString(data).getAsJsonObject();
            String endpoint = eventData.has("endpoint") ? eventData.get("endpoint").getAsString() : "default";
            
            MinescriptMessageHandler handler = handlers.get(endpoint);
            if (handler != null) {
                handler.handle(endpoint, data);
            }
            
            // Also check for command-specific handlers (preserves bridge routing)
            if ("command".equals(eventType)) {
                MinescriptMessageHandler cmdHandler = handlers.get("cmd");
                if (cmdHandler != null) {
                    cmdHandler.handle("cmd", data);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling Minescript event: " + e.getMessage());
        }
    }
    
    /**
     * Publish event to n8n (replaces MQTT publish)
     */
    public void publish(String endpoint, String json) {
        if (!connected) return;
        
        try {
            String enrichedJson = ensureSessionId(json);
            JsonObject payload = new JsonObject();
            payload.addProperty("endpoint", endpoint);
            payload.add("data", JsonParser.parseString(enrichedJson));
            payload.addProperty("timestamp", System.currentTimeMillis());
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                .url(cfg.minescriptBaseUrl + "/events")
                .addHeader("Authorization", "Bearer " + cfg.authToken)
                .addHeader("X-Session-ID", cfg.sessionId)
                .post(body)
                .build();
                
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Silent failure for async publish
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            // Silent failure for async publish
        }
    }
    
    /**
     * Synchronous publish with timeout (replaces MQTT publishSync)
     */
    public void publishSync(String endpoint, String json, int timeoutMs) {
        if (!connected) return;
        
        try {
            String enrichedJson = ensureSessionId(json);
            JsonObject payload = new JsonObject();
            payload.addProperty("endpoint", endpoint);
            payload.add("data", JsonParser.parseString(enrichedJson));
            payload.addProperty("timestamp", System.currentTimeMillis());
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                .url(cfg.minescriptBaseUrl + "/events")
                .addHeader("Authorization", "Bearer " + cfg.authToken)
                .addHeader("X-Session-ID", cfg.sessionId)
                .post(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                // Ensure response is consumed
            }
        } catch (Exception e) {
            // Silent failure
        }
    }
    
    /**
     * Ensure session_id is present in JSON payload
     */
    private String ensureSessionId(String json) {
        if (cfg.sessionId == null || cfg.sessionId.isBlank()) return json;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return json;
            JsonObject obj = el.getAsJsonObject();
            
            if (!obj.has("session_id")) {
                obj.addProperty("session_id", cfg.sessionId);
            }
            return obj.toString();
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * Publish status event (replaces MQTT status announcements)
     */
    private void publishStatus(String status) {
        publish("events", JsonUtils.baseEvent("status", status, cfg.sessionId));
    }
    
    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before reconnect
                if (!connected) {
                    connect();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Gracefully close connection
     */
    public void close() {
        try {
            if (eventSource != null) {
                eventSource.cancel();
                eventSource = null;
            }
            connected = false;
        } catch (Exception e) {
            // Silent cleanup
        }
    }
}
