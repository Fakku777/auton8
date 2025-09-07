package com.kilab.auton8.modules;

import com.kilab.auton8.Auton8;
import com.kilab.auton8.core.Auton8Core;
import com.kilab.auton8.core.Config;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.UUID;

public class MqttLinkModule extends Module {
    // ===== Hard-coded connection details =====
    private static final String BROKER_URI = "tcp://127.0.0.1:1883";
    private static final String CLIENT_ID  = "kilab-pc1";
    private static final String USERNAME   = "kilab-pc1";
    private static final String PASSWORD   = "YOUR_SUPER_STRONG_PASSWORD";
    private static final String CMD_TOPIC  = "mc/kilab-pc1/cmd";
    private static final String EVT_TOPIC  = "mc/kilab-pc1/events";

    // ===== User-facing feature toggles (live) =====
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> allowChatRx = sg.add(new BoolSetting.Builder()
        .name("chat-receive").description("Publish received chat to MQTT").defaultValue(true).build());

    private final Setting<Boolean> allowChatTx = sg.add(new BoolSetting.Builder()
        .name("chat-send").description("Allow agent to send chat via MQTT").defaultValue(true).build());

    private final Setting<Boolean> allowTelemetry = sg.add(new BoolSetting.Builder()
        .name("telemetry").description("Publish position/health status").defaultValue(true).build());

    private final Setting<Boolean> allowBaritoneCmds = sg.add(new BoolSetting.Builder()
        .name("baritone-cmds").description("Accept # commands from MQTT").defaultValue(true).build());

    private final Setting<Integer> telemetryMs = sg.add(new IntSetting.Builder()
        .name("telemetry-interval-ms").defaultValue(5000).min(250).sliderMax(15000).build());

    // ===== Runtime =====
    private Auton8Core core;

    // announce session_start on first tick after enable (safer for async connects)
    private boolean sessionHelloPending = false;
    private String  currentSessionId = null;

    // last-applied cache so we can apply only diffs
    private boolean lastChatRx, lastChatTx, lastTel, lastBaritone;
    private int     lastTelemMs;

    public MqttLinkModule() {
        super(Auton8.CAT, "n8n connection", "Bridges Minecraft events <-> MQTT (Auton8).");
    }

    @Override
    public void onActivate() {
        // Build config
        Config cfg = new Config();
        cfg.brokerUri = BROKER_URI;
        cfg.clientId  = CLIENT_ID;
        cfg.username  = USERNAME;
        cfg.password  = PASSWORD;
        cfg.cmdTopic  = CMD_TOPIC;
        cfg.evtTopic  = EVT_TOPIC;

        // Fresh session on each enable → n8n can hard-reset its global state
        currentSessionId = UUID.randomUUID().toString();
        cfg.sessionId = currentSessionId;

        // Initial flags from settings
        cfg.allowChatRx         = allowChatRx.get();
        cfg.allowChatTx         = allowChatTx.get();
        cfg.allowTelemetry      = allowTelemetry.get();
        cfg.allowBaritone       = allowBaritoneCmds.get();
        cfg.telemetryIntervalMs = telemetryMs.get();

        // Bring up core (connect MQTT, start bridges)
        core = new Auton8Core(cfg);
        core.enable();

        // Ask to announce session_start on the next tick (post-connect)
        sessionHelloPending = true;

        // Seed caches
        lastChatRx   = cfg.allowChatRx;
        lastChatTx   = cfg.allowChatTx;
        lastTel      = cfg.allowTelemetry;
        lastBaritone = cfg.allowBaritone;
        lastTelemMs  = cfg.telemetryIntervalMs;
    }

    @Override
    public void onDeactivate() {
        if (core != null) {
            // Tell n8n the session is over (lets it clear timers/status)
            try {
                core.emitSessionEnd();
            } catch (Throwable ignored) { /* best-effort */ }
            core.disable();
            core = null;
        }
        sessionHelloPending = false;
        currentSessionId = null;
    }

    // Apply setting changes LIVE (no re-toggle needed) + send session_start once
    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (core == null) return;

        // One-time hello after enable (safer if MQTT connection takes a moment)
        if (sessionHelloPending) {
            try {
                core.emitSessionStart();          // must publish {event:"session_start", session_id, ...}
                // Optional: immediately push a telemetry packet so n8n sees the new session_id on data
                try { core.requestOneShotTelemetry(); } catch (Throwable ignored) {}
                sessionHelloPending = false;
            } catch (Throwable t) {
                // keep pending; we'll try again next tick
                return;
            }
        }

        // Live flag diffs
        boolean vChatRx   = allowChatRx.get();
        boolean vChatTx   = allowChatTx.get();
        boolean vTel      = allowTelemetry.get();
        boolean vBaritone = allowBaritoneCmds.get();
        int     vTelemMs  = telemetryMs.get();

        if (vChatRx != lastChatRx) {
            core.setAllowChatRx(vChatRx);
            lastChatRx = vChatRx;
        }
        if (vChatTx != lastChatTx) {
            core.setAllowChatTx(vChatTx);
            lastChatTx = vChatTx;
        }
        if (vTel != lastTel) {
            core.setAllowTelemetry(vTel);
            lastTel = vTel;
        }
        if (vBaritone != lastBaritone) {
            core.setAllowBaritone(vBaritone);
            lastBaritone = vBaritone;
        }
        if (vTelemMs != lastTelemMs) {
            core.setTelemetryIntervalMs(vTelemMs);
            lastTelemMs = vTelemMs;
        }
    }
}
