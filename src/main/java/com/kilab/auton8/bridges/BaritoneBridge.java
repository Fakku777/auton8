package com.kilab.auton8.bridges;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kilab.auton8.core.Config;
import com.kilab.auton8.core.JsonUtils;
import com.kilab.auton8.mqtt.MqttBus;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

public final class BaritoneBridge implements Bridge {
    private final Config cfg;
    private final MqttBus bus;

    // ---- runtime (no Baritone API types) ----
    private String state = "IDLE";                 // IDLE|PATHING|STUCK
    private String lastCmd = null;
    private long   lastCmdStartMs = 0L;
    private String lastCmdOutcome = "pending";     // pending|success|fail
    private String lastReason = "none";            // none|stuck|goal_reached|cancelled|interrupted

    private int retries = 0;
    private int cooldownTicks = 0;                 // anti-thrash (ticks @20TPS)

    private BlockPos target = null;
    private String   targetKey = null;             // "x:y:z" latch for this goal
    private double   distanceRemaining = -1.0;     // HORIZONTAL (XZ) remaining distance

    // motion buffer
    private final Deque<Vec3d> posRing = new ArrayDeque<>();
    private long lastPublishMs = 0L;

    // accept / pathing detection (robust to latency)
    private boolean awaitingAccept = false;
    private long    acceptDeadlineMs = 0L;
    private boolean acceptedEmitted = false;
    private long    movingSinceMs   = 0L;          // when sustained movement began

    // goal debounce
    private boolean goalEmittedForThisTarget = false;
    private boolean withinGoalNow = false;
    private long    withinGoalSinceMs = 0L;
    private long    lastGoalEmitMs = 0L;

    // strict stuck rule: last time we had horizontal motion
    private long lastHorizontalMoveMs;

    // ------------------ KNOBS ------------------
    private static final int    PUBLISH_INTERVAL_MS   = 950;
    private static final int    RING_MAX              = 20;        // ~1s at 20 TPS
    private static final int    COOLDOWN_TICKS        = 20 * 8;    // 8s after retry
    private static final int    MAX_RETRIES           = 3;

    private static final double GOAL_EPS_XZ           = 3.0;       // horizontal 3m = reached
    private static final long   GOAL_STAY_MS          = 1_200;     // must stay within eps for 1.2s
    private static final long   GOAL_REEMIT_DEBOUNCE  = 10_000;    // never re-emit sooner than this

    private static final double MOVING_SPEED_MPS      = 0.4;       // for PATHING recognition
    private static final double HORIZ_MOVE_EPS_SPEED  = 0.05;      // any horizontal motion

    // Accept windows: servers can be laggy → give Baritone/serv ices time
    private static final long   ACCEPT_WINDOW_MS      = 15_000;    // to start pathing
    private static final long   ACCEPT_SUSTAIN_MS     = 1_500;     // must move this long before "accepted"

    // *** ONLY stuck trigger: NO horizontal movement for this long (X/Z only) ***
    private static final long   STUCK_IDLE_MS         = 20_000;    // 20 seconds (unchanged)

    public BaritoneBridge(Config cfg, MqttBus bus) {
        this.cfg = cfg;
        this.bus = bus;
        this.lastHorizontalMoveMs = System.currentTimeMillis();
    }

    @Override
    public void enable() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (cooldownTicks > 0) cooldownTicks--;
                samplePos(client);
                updateHeuristicState(client);   // sticky state + emits cmd_accepted/cmd_reject (debounced)
                maybeDetectGoalOrStuck();       // emits goal_reached (debounced) / stuck_detected (+ retries)
                maybePublishSnapshot();         // periodic baritone_state snapshot
            } catch (Exception e) {
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("error", e.getClass().getSimpleName()));
            }
        });
    }

    @Override public void disable() { }

    // ---------- locally typed "#..." forwarded here from ChatBridge ----------
    // IMPORTANT: This path NEVER re-sends chat and NEVER publishes "accepted".
    public void onLocalBaritoneCommand(String raw) {
        if (!cfg.allowBaritone || raw == null || !raw.startsWith("#")) return;

        String low = raw.toLowerCase().trim();
        BlockPos newTarget = parseGoto(low);

        armNewCommand(raw, newTarget);

        if (low.equals("#cancel") || low.equals("#stop")) {
            clearCurrentGoalContext();
            state = "IDLE";
        }
    }

    // ---------- commands in (from MQTT) ----------
    @Override
    public void onCommand(String json) {
        if (!cfg.allowBaritone) return;
        try {
            JsonObject j = JsonParser.parseString(json).getAsJsonObject();
            if (!"baritone_cmd".equals(j.get("type").getAsString())) return;

            String cmd = j.has("cmd") ? j.get("cmd").getAsString() : null;
            if (cmd == null || !cmd.startsWith("#") || cmd.length() > 120) {
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("cmd_reject","bad_cmd"));
                return;
            }

            final String low = cmd.toLowerCase().trim();
            final BlockPos newTarget = parseGoto(low);
            final String send = cmd;

            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                if (mc.player == null || mc.player.networkHandler == null) {
                    bus.publish(cfg.evtTopic, JsonUtils.baseEvent("cmd_reject","no_player"));
                    return;
                }

                // Suppress local hook so ChatBridge doesn't feed this back as "local"
                ChatBridge.SUPPRESS_LOCAL_BARITONE_HOOK.set(Boolean.TRUE);
                try {
                    mc.player.networkHandler.sendChatMessage(send);
                } finally {
                    ChatBridge.SUPPRESS_LOCAL_BARITONE_HOOK.set(Boolean.FALSE);
                }

                armNewCommand(send, newTarget);

                // retry bookkeeping
                if (send.equalsIgnoreCase("#path")) retries++;
                if (send.equalsIgnoreCase("#cancel") || send.equalsIgnoreCase("#stop")) {
                    clearCurrentGoalContext();
                    state = "IDLE";
                }

                // legacy trace (not used by router)
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("accepted", send));
            });
        } catch (Exception e) {
            bus.publish(cfg.evtTopic, JsonUtils.baseEvent("error", e.getClass().getSimpleName()));
        }
    }

    // ---------- internals ----------
    private void armNewCommand(String send, BlockPos newTarget) {
        // bookkeeping
        lastCmd = send;
        lastCmdStartMs = System.currentTimeMillis();
        lastCmdOutcome = "pending";
        lastReason = "none";
        distanceRemaining = -1.0;

        if (newTarget != null) {
            target = newTarget;
            targetKey = target.getX() + ":" + target.getY() + ":" + target.getZ();
        }

        // acceptance window & flags
        awaitingAccept = true;
        acceptedEmitted = false;
        acceptDeadlineMs = lastCmdStartMs + ACCEPT_WINDOW_MS;
        movingSinceMs = 0L;

        // new goal latch
        goalEmittedForThisTarget = false;
        withinGoalNow = false;
        withinGoalSinceMs = 0L;
    }

    private static BlockPos parseGoto(String low) {
        if (!low.startsWith("#goto")) return null;
        String[] parts = low.split("\\s+");
        if (parts.length < 4) return null;
        try {
            int x = (int)Math.round(Double.parseDouble(parts[1]));
            int y = (int)Math.round(Double.parseDouble(parts[2]));
            int z = (int)Math.round(Double.parseDouble(parts[3]));
            return new BlockPos(x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void samplePos(MinecraftClient mc) {
        if (mc.player == null) return;
        Vec3d p = mc.player.getPos();
        posRing.addLast(p);
        while (posRing.size() > RING_MAX) posRing.removeFirst();
    }

    // ---------- speed helpers (HORIZONTAL only) ----------
    private double avgHorizontalSpeedMps() {
        if (posRing.size() < 2) return 0.0;
        Vec3d first = posRing.getFirst(), last = posRing.getLast();
        double dx = last.x - first.x;
        double dz = last.z - first.z;
        double distXZ = Math.hypot(dx, dz);
        double secs = Math.max(1.0, (posRing.size() - 1) / 20.0);
        return distXZ / secs;
    }

    private void updateHeuristicState(MinecraftClient mc) {
        // compute horizontal distanceRemaining (ignore Y)
        if (target != null && mc.player != null) {
            Vec3d p = mc.player.getPos();
            double dx = (target.getX() + 0.5) - p.x;
            double dz = (target.getZ() + 0.5) - p.z;
            distanceRemaining = Math.hypot(dx, dz);
        } else {
            distanceRemaining = -1.0;
        }

        // update lastHorizontalMoveMs whenever we are moving a little on X/Z
        double speed = avgHorizontalSpeedMps();
        long now = System.currentTimeMillis();
        if (speed >= HORIZ_MOVE_EPS_SPEED) {
            lastHorizontalMoveMs = now;
        }

        // qualify movement as "pathing" only if sustained for ACCEPT_SUSTAIN_MS
        boolean movingFastEnough = speed >= MOVING_SPEED_MPS;

        if (awaitingAccept && movingFastEnough) {
            if (movingSinceMs == 0L) movingSinceMs = now;                   // start window
            if (!acceptedEmitted && now - movingSinceMs >= ACCEPT_SUSTAIN_MS) {
                // Emit accepted ONCE per command
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("cmd_accepted", lastCmd == null ? "" : lastCmd));
                awaitingAccept = false;
                acceptedEmitted = true;
                state = "PATHING";
            }
        } else {
            // lost movement → reset sustain timer (but do not re-arm emit)
            movingSinceMs = 0L;
        }

        // Accept window timeout → single reject
        if (awaitingAccept && now > acceptDeadlineMs && !acceptedEmitted) {
            bus.publish(cfg.evtTopic, JsonUtils.baseEvent("cmd_reject", "timeout_no_pathing"));
            awaitingAccept = false;
        }

        // Update sticky state (PATHING only when actually moving)
        if (movingFastEnough) state = "PATHING";
        else if ("STUCK".equals(state)) { /* keep STUCK until cleared */ }
        else state = "IDLE";
    }

    private void maybeDetectGoalOrStuck() {
        long now = System.currentTimeMillis();

        // ---- GOAL REACHED (HORIZONTAL, debounced) ----
        if (target != null && distanceRemaining >= 0) {
            boolean within = distanceRemaining <= GOAL_EPS_XZ;

            if (within && !withinGoalNow) {
                withinGoalNow = true;
                withinGoalSinceMs = now;
            } else if (!within) {
                withinGoalNow = false;
                withinGoalSinceMs = 0L;
            }

            if (withinGoalNow
                && !goalEmittedForThisTarget
                && (now - withinGoalSinceMs) >= GOAL_STAY_MS
                && (now - lastGoalEmitMs) >= GOAL_REEMIT_DEBOUNCE) {

                lastCmdOutcome = "success";
                lastReason = "goal_reached";
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("goal_reached", target.toShortString()));

                // latch
                goalEmittedForThisTarget = true;
                lastGoalEmitMs = now;

                // fully clear command/acceptance context so no re-accepts
                clearCurrentGoalContext();

                retries = 0;
                state = "IDLE";
                return;
            }
        }

        // ---- ONLY stuck condition: no horizontal (X/Z) movement for 20s ----
        // (kept identical semantics to your existing logic)
        long sinceHorizMove = now - lastHorizontalMoveMs;
        if (sinceHorizMove >= STUCK_IDLE_MS) {
            state = "STUCK";
            lastReason = "stuck";
            bus.publish(cfg.evtTopic,
                JsonUtils.baseEvent("stuck_detected", String.valueOf((int)Math.round(distanceRemaining)))
            );

            // Optional retry: only useful if last command was a goto
            if (cooldownTicks == 0 && lastCmd != null && lastCmd.toLowerCase().startsWith("#goto") && retries < MAX_RETRIES) {
                sendClientChat("#path");
                retries++;
                cooldownTicks = COOLDOWN_TICKS;

                // re-arm accept window for the retry
                awaitingAccept = true;
                acceptedEmitted = false;
                acceptDeadlineMs = System.currentTimeMillis() + ACCEPT_WINDOW_MS;
                movingSinceMs = 0L;
            } else if (retries >= MAX_RETRIES) {
                lastCmdOutcome = "fail";
            }

            // reset the timer so we don't spam stuck every tick
            lastHorizontalMoveMs = now;
        }
    }

    private void clearCurrentGoalContext() {
        target = null;
        targetKey = null;
        distanceRemaining = -1.0;

        awaitingAccept = false;
        acceptedEmitted = false;
        acceptDeadlineMs = 0L;
        movingSinceMs = 0L;

        // clear command so later movement can't be treated as acceptance of an old goto
        lastCmd = null;
        lastCmdStartMs = 0L;

        // reset goal debounce window for the next target
        withinGoalNow = false;
        withinGoalSinceMs = 0L;
        goalEmittedForThisTarget = false;
    }

    private void maybePublishSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastPublishMs < PUBLISH_INTERVAL_MS) return;
        lastPublishMs = now;

        double speed = avgHorizontalSpeedMps(); // horizontal speed only
        long elapsedSec = lastCmdStartMs == 0 ? 0 : Math.max(0, (now - lastCmdStartMs) / 1000);

        JsonObject snap = new JsonObject();
        snap.addProperty("ts", now / 1000);
        snap.addProperty("state", state);
        if (lastCmd != null) snap.addProperty("lastCmd", lastCmd);
        snap.addProperty("lastCmdOutcome", lastCmdOutcome);
        snap.addProperty("reason", lastReason);
        snap.addProperty("elapsedSec", elapsedSec);
        snap.addProperty("retries", retries);
        snap.addProperty("cooldownSec", cooldownTicks / 20);
        snap.addProperty("speedAvg", Math.round(speed * 100.0) / 100.0);
        snap.addProperty("distanceRemaining", distanceRemaining);
        snap.addProperty("awaitingAccept", awaitingAccept);
        snap.addProperty("acceptedEmitted", acceptedEmitted);
        snap.addProperty("movingSinceMs", movingSinceMs);
        snap.addProperty("withinGoalNow", withinGoalNow);
        snap.addProperty("withinGoalForMs", withinGoalNow ? (now - withinGoalSinceMs) : 0);
        snap.addProperty("lastGoalEmitMsAgo", lastGoalEmitMs == 0 ? -1 : (now - lastGoalEmitMs));

        if (target != null) {
            JsonObject t = new JsonObject();
            t.addProperty("x", target.getX());
            t.addProperty("y", target.getY());
            t.addProperty("z", target.getZ());
            t.addProperty("key", targetKey);
            snap.add("target", t);
        }

        String topic = cfg.baritoneStateTopicOrDefault();
        bus.publish(topic, JsonUtils.wrap("baritone_state", snap));
    }

    private void sendClientChat(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                // Suppress ChatBridge local hook so we don't re-enter onLocalBaritoneCommand
                ChatBridge.SUPPRESS_LOCAL_BARITONE_HOOK.set(Boolean.TRUE);
                try {
                    mc.player.networkHandler.sendChatMessage(msg);
                } finally {
                    ChatBridge.SUPPRESS_LOCAL_BARITONE_HOOK.set(Boolean.FALSE);
                }
                // legacy "accepted" trace
                bus.publish(cfg.evtTopic, JsonUtils.baseEvent("accepted", msg));

                // re-arm accept window for any programmatic resend (#path)
                lastCmd = msg;
                lastCmdStartMs = System.currentTimeMillis();
                awaitingAccept = true;
                acceptedEmitted = false;
                acceptDeadlineMs = lastCmdStartMs + ACCEPT_WINDOW_MS;
                movingSinceMs = 0L;
            }
        });
    }
}
