package com.april777.auton8.bridges;

import com.april777.auton8.core.Config;
import com.april777.auton8.core.JsonUtils;
import com.april777.auton8.minescript.MinescriptClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public final class LifeBridge implements Bridge {
    private final Config cfg; private final MinescriptClient client;

    private boolean ticking = false;
    private Boolean wasAlive = null;        // null until first sample
    private long lastEventMs = 0L;          // debounce
    private static final long MIN_EVENT_GAP_MS = 750;  // avoid dup spam

    public LifeBridge(Config cfg, MinescriptClient client) { this.cfg = cfg; this.client = client; }

    @Override public void enable() {
        if (ticking) return;
        ticking = true;

        ClientTickEvents.END_CLIENT_TICK.register(mcClient -> {
            if (!ticking) return;
            var mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.world == null) return;

            final boolean alive = mc.player.isAlive() && mc.player.getHealth() > 0f;
            if (wasAlive == null) {
                wasAlive = alive;                 // initialize silently
                return;
            }
            if (alive == wasAlive) return;        // no transition

            final long now = System.currentTimeMillis();
            if (now - lastEventMs < MIN_EVENT_GAP_MS) return;  // debounce

            lastEventMs = now;
            wasAlive = alive;

            final double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
            final String world = mc.isIntegratedServerRunning() ? "singleplayer" : "server";

            if (!alive) {
                // DEAD -> publish death
                this.client.publish(cfg.evtEndpoint, JsonUtils.life("dead", world, x, y, z, /*cause*/ null));
            } else {
                // RESPAWNED -> publish respawn
                this.client.publish(cfg.evtEndpoint, JsonUtils.life("respawned", world, x, y, z, null));
            }
        });
    }

    @Override public void disable() { ticking = false; }

    @Override public void onCommand(String json) {
        // no commands needed for life sensor
    }
}
