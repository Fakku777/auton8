package com.april777.auton8.bridges;

import com.google.gson.JsonObject;
import com.april777.auton8.core.Config;
import com.april777.auton8.core.JsonUtils;
import com.april777.auton8.minescript.MinescriptClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.UUID;

public final class ConnectionBridge implements Bridge {
    private final Config cfg;
    private final MinescriptClient client;
    private boolean hooked = false;

    // Persist a session id per world/server connection
    private String sessionId = null;

    public ConnectionBridge(Config cfg, MinescriptClient client) {
        this.cfg = cfg;
        this.client = client;
    }

    @Override
    public void enable() {
        if (hooked) return;
        hooked = true;

        // Fired when we connect to a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            sessionId = UUID.randomUUID().toString();

            // status: connected (with session_id)
            JsonObject ev = JsonUtils.baseEventObj("status", "connected", sessionId);
            ev.addProperty("reset", true); // signal a fresh stretch if you want
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(ev));

            // one-shot session_start (same session_id)
            JsonObject start = JsonUtils.baseEventObj("session_start", "begin", sessionId);
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(start));

            // (optional) legacy "world":"joined"
            JsonObject legacy = JsonUtils.baseEventObj("world", "joined");
            legacy.addProperty("session_id", sessionId);
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(legacy));
        });

        // Fired when we disconnect from a world/server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            // status: disconnected (with last session_id)
            JsonObject ev = JsonUtils.baseEventObj("status", "disconnected");
            if (sessionId != null) ev.addProperty("session_id", sessionId);
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(ev));

            // one-shot session_end
            JsonObject end = JsonUtils.baseEventObj("session_end", "end");
            if (sessionId != null) end.addProperty("session_id", sessionId);
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(end));

            // (optional) legacy "world":"left"
            JsonObject legacy = JsonUtils.baseEventObj("world", "left");
            if (sessionId != null) legacy.addProperty("session_id", sessionId);
            this.client.publish(cfg.evtEndpoint, JsonUtils.wrap(legacy));

            // clear local session id
            sessionId = null;
        });
    }

    @Override public void disable() { /* no-op */ }

    @Override public void onCommand(String json) { /* no-op */ }
}
