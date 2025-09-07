package com.kilab.auton8.hud;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class HudStatusStore {
    public static final class Target { public int x, y, z; }
    public static final class Goal   { public String desc; public String dimension; public Target target; }
    public static final class PlayerInfo { public String name; public Double dist; } // <- was Integer

    // Matches n8n HUD JSON
    public static final class Status {
        public String  title = "Auton8";

        public String  agent_status;                 // alias from n8n
        public String  status = "idle";              // idle | active | requesting | await_accept

        public String  world = "";
        public String  dimension;                    // overworld | nether | end
        public Integer distance_from_spawn;          // blocks
        public Double  speed_bps;                    // blocks/s

        public Goal    goal;                         // { desc, dimension, target }
        public Target  target;                       // legacy flat target (still accepted)
        public Integer step_remaining;               // remaining blocks

        public Integer cooldown_sec;                 // optional
        public Boolean danger;                       // optional
        public PlayerInfo last_player;               // optional

        public String  message = "";

        // Planner strings for HUD
        public String  planner_reason;               // single, concise reason
        public String  goal_expl;                    // multi-sentence goal explanation (optional)

        public long    lastUpdateMs = 0L;
    }

    private static final AtomicReference<Status> LAST =
        new AtomicReference<>(new Status());

    public static HudStatusStore.Status snapshot() { return LAST.get(); }

    // ---- updates ----
    private static final com.google.gson.Gson GSON =
        new com.google.gson.GsonBuilder().setLenient().create();

    public static void updateFromJson(byte[] payload) {
        if (payload == null) return;
        updateFromJson(new String(payload, StandardCharsets.UTF_8));
    }

    public static void updateFromJson(String json) {
        try {
            var raw = GSON.fromJson(json, Status.class);
            if (raw != null) {
                // Mirror agent_status → status when status is empty
                if ((raw.status == null || raw.status.isBlank()) && raw.agent_status != null) {
                    raw.status = raw.agent_status;
                }
                raw.lastUpdateMs = System.currentTimeMillis();
                LAST.set(raw);
            }
        } catch (Exception ignored) { /* swallow bad payloads */ }
    }
}
