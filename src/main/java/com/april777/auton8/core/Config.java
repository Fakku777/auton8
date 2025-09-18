package com.april777.auton8.core;

public class Config {
    // Minescript API
    public String minescriptBaseUrl;
    public String clientId;
    public String authKey;
    public String authToken;  // Runtime token from authentication
    public String cmdEndpoint;
    public String evtEndpoint;

    // Session — new run identifier (set on module enable)
    public String sessionId;   // e.g., UUID string

    // HUD endpoint (optional override)
    public String hudEndpoint;  // e.g. "hud"

    // Where BaritoneBridge publishes snapshots
    public String stateEndpointBaritone;
    
    // Docker management
    public boolean autoStartDocker = true;
    public String dockerProjectDir;

    // Scopes
    public boolean allowChatRx;
    public boolean allowChatTx;
    public boolean allowTelemetry;
    public boolean allowBaritone;

    // Telemetry
    public int telemetryIntervalMs = 5000;

    public Config copy() {
        Config c = new Config();
        c.minescriptBaseUrl = minescriptBaseUrl;
        c.clientId = clientId;
        c.authKey = authKey;
        c.authToken = authToken;
        c.cmdEndpoint = cmdEndpoint;
        c.evtEndpoint = evtEndpoint;

        c.sessionId = sessionId;                 // Copy session id

        c.hudEndpoint = hudEndpoint;
        c.stateEndpointBaritone = stateEndpointBaritone;
        c.autoStartDocker = autoStartDocker;
        c.dockerProjectDir = dockerProjectDir;

        c.allowChatRx = allowChatRx;
        c.allowChatTx = allowChatTx;
        c.allowTelemetry = allowTelemetry;
        c.allowBaritone = allowBaritone;

        c.telemetryIntervalMs = telemetryIntervalMs;
        return c;
    }

    // Defaults
    public String hudEndpointOrDefault() {
        return (hudEndpoint != null && !hudEndpoint.isBlank())
            ? hudEndpoint
            : "hud";
    }

    public String baritoneStateEndpointOrDefault() {
        return (stateEndpointBaritone != null && !stateEndpointBaritone.isBlank())
            ? stateEndpointBaritone
            : "baritone_state";
    }
}
