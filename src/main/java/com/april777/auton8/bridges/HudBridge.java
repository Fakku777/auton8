package com.april777.auton8.bridges;

import com.april777.auton8.core.Config;
import com.april777.auton8.hud.Auton8Hud;
import com.april777.auton8.hud.HudStatusStore;
import com.april777.auton8.minescript.MinescriptClient;
import com.april777.auton8.minescript.MinescriptMessageHandler;
import meteordevelopment.meteorclient.systems.hud.Hud;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HudBridge {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private final MinescriptClient client;
    private final String hudEndpoint;

    public HudBridge(MinescriptClient client, Config cfg) {
        this.client = client;
        this.hudEndpoint = cfg.hudEndpointOrDefault(); // "hud" by default
    }

    public void start() {
        // Register the HUD element only once per client session.
        if (REGISTERED.compareAndSet(false, true)) {
            Hud.get().register(Auton8Hud.INFO);
        }

        // Subscribe to HUD updates (JSON → in-memory snapshot)
        client.onMessage(hudEndpoint, (MinescriptMessageHandler) (String endpoint, String payload) ->
            HudStatusStore.updateFromJson(payload)
        );
    }

    public void stop() {
        // Meteor HUD doesn't expose unregister; safe to leave registered.
    }
}
