package com.april777.auton8.minescript;

/**
 * Message handler interface for Minescript communication
 * Direct replacement for MqttMessageHandler to maintain bridge compatibility
 */
@FunctionalInterface
public interface MinescriptMessageHandler {
    /**
     * Handle incoming message from n8n via Minescript API
     * 
     * @param endpoint The endpoint that received the message (replaces MQTT topic)
     * @param json The JSON message content
     */
    void handle(String endpoint, String json);
}
