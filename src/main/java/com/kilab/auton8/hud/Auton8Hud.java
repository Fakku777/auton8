package com.kilab.auton8.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Auton8Hud extends HudElement {
    public static final HudElementInfo<Auton8Hud> INFO =
        new HudElementInfo<>(Hud.GROUP, "auton8",
            "Shows Auton8 planner/agent status with tidy layout.", Auton8Hud::new);

    // ---------- colors ----------
    private static final Color TEXT        = Color.WHITE;
    private static final Color SUBTLE      = new Color(170,170,170);
    private static final Color REASON_COL  = new Color(180,210,180);
    private static final Color ACCENT_OK   = new Color( 90,220,120);
    private static final Color ACCENT_WAIT = new Color(240,200, 80);
    private static final Color ACCENT_IDLE = new Color(140,160,180);
    private static final Color ACCENT_DNG  = new Color(240,100,100);

    // ---------- settings ----------
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Integer> maxWidth = sg.add(new IntSetting.Builder()
        .name("max-width")
        .description("Maximum HUD width before wrapping (px).")
        .defaultValue(520)
        .min(220).max(900)
        .build());

    private final Setting<Integer> padding = sg.add(new IntSetting.Builder()
        .name("padding")
        .description("Inner padding (px).")
        .defaultValue(6)
        .min(0).max(20)
        .build());

    private final Setting<Boolean> compact = sg.add(new BoolSetting.Builder()
        .name("compact")
        .description("Tighter spacing and shorter labels.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> showBackground = sg.add(new BoolSetting.Builder()
        .name("background")
        .description("Draw a background behind the HUD.")
        .defaultValue(true)
        .build());

    private final Setting<SettingColor> bgColor = sg.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background fill color.")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .visible(showBackground::get)
        .build());

    private final Setting<Boolean> dropShadow = sg.add(new BoolSetting.Builder()
        .name("drop-shadow")
        .description("Draw a soft drop shadow behind the background.")
        .defaultValue(true)
        .visible(showBackground::get)
        .build());

    private final Setting<Integer> shadowOffset = sg.add(new IntSetting.Builder()
        .name("shadow-offset")
        .description("Drop shadow offset (px).")
        .defaultValue(3)
        .min(1).max(8)
        .visible(() -> showBackground.get() && dropShadow.get())
        .build());

    private final Setting<SettingColor> shadowColor = sg.add(new ColorSetting.Builder()
        .name("shadow-color")
        .description("Drop shadow color.")
        .defaultValue(new SettingColor(0, 0, 0, 90))
        .visible(() -> showBackground.get() && dropShadow.get())
        .build());

    private final Setting<Boolean> showStatusLine = sg.add(new BoolSetting.Builder()
        .name("show-status-line")
        .description("Show the transient status line if present (e.g., Danger, Waiting for Baritone).")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> showGoalExpl = sg.add(new BoolSetting.Builder()
        .name("show-goal-explanation")
        .description("Show the multi-sentence goal explanation if present.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> showLastPlayer = sg.add(new BoolSetting.Builder()
        .name("show-last-player")
        .description("Show last seen player and distance if present.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> showReason = sg.add(new BoolSetting.Builder()
        .name("show-reason")
        .description("Show the planner's short reason line.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> colGutter = sg.add(new IntSetting.Builder()
        .name("column-gutter")
        .description("Gap between left/right columns (px).")
        .defaultValue(10)
        .min(4).max(40)
        .build());

    public Auton8Hud() { super(INFO); }

    @Override
    public void render(HudRenderer r) {
        var s = HudStatusStore.snapshot();

        // No data yet
        if (s.lastUpdateMs <= 0) {
            String msg = "Auton8 — waiting for HUD data…";
            double w = clamp(maxWidth.get(), r.textWidth(msg) + padding.get() * 2);
            double h = r.textHeight() + padding.get() * 2;
            setSize(w, h);
            drawBackground(r, w, h);
            r.text(msg, x + padding.get(), y + padding.get(), SUBTLE, true);
            return;
        }

        // --- state color
        String state = safe(s.status, "idle").toLowerCase(Locale.ROOT);
        boolean danger = Boolean.TRUE.equals(s.danger);
        Color stateColor = danger ? ACCENT_DNG :
            switch (state) {
                case "active" -> ACCENT_OK;
                case "await_accept", "requesting" -> ACCENT_WAIT;
                case "idle" -> ACCENT_IDLE;
                default -> TEXT;
            };

        // --- header
        String title = safe(s.title, "Auton8");
        String header = title + "  •  " + state;

        // --- status line (optional)
        String statusLine = null;
        String msg = safe(s.message, "").trim();
        if (showStatusLine.get() && !msg.isEmpty() && !msg.equalsIgnoreCase(state)) {
            statusLine = (danger ? "⚠ " : "") + msg;
        } else if (showStatusLine.get() && danger) statusLine = "⚠ Danger";

        // --- world / metrics (left column)
        String dim = shortDim(s.dimension != null ? s.dimension :
            (s.goal != null ? s.goal.dimension : null));
        String world = safe(s.world, "—");
        String dist  = fmtInt(s.distance_from_spawn);
        String speed = fmtSpeed(s.speed_bps);
        String worldA = "[" + dim + "] " + world;
        String worldB = (compact.get() ? "Dist " : "Distance ") + dist + "  •  " + speed;

        // --- goal / target (right column)
        var tgt = s.goal != null && s.goal.target != null ? s.goal.target : s.target;
        String goalHead = (s.goal != null && s.goal.desc != null && !s.goal.desc.isBlank())
            ? s.goal.desc.trim()
            : (tgt != null ? "Travel" : "Goal");
        String remaining = fmtInt(s.step_remaining);
        String goalA = "Goal: " + safe(goalHead, "—");
        String goalB = (tgt != null)
            ? "Target: (" + tgt.x + "," + tgt.y + "," + tgt.z + ")  ~" + remaining
            : "Target: —";

        // --- extras (single-column)
        String playerLine = null;
        if (showLastPlayer.get() && s.last_player != null &&
            (safe(s.last_player.name, "").length() > 0 || s.last_player.dist != null)) {
            String pDist = s.last_player.dist != null
                ? String.format(Locale.US, "%.0fm", Math.abs(s.last_player.dist))
                : "—";
            playerLine = "Last player: " + safe(s.last_player.name, "unknown") + "  (" + pDist + ")";
        }

        String reasonLine = (showReason.get() && s.planner_reason != null && !s.planner_reason.isBlank())
            ? "Reason: " + s.planner_reason.trim()
            : null;

        String goalExplLine = (showGoalExpl.get() && s.goal_expl != null && !s.goal_expl.isBlank())
            ? s.goal_expl.trim()
            : null;

        long ageMs = Math.max(0, System.currentTimeMillis() - s.lastUpdateMs);
        String age = "⏱ " + (ageMs < 1000 ? ageMs + "ms"
            : String.format(Locale.US, "%.1fs", ageMs / 1000.0))
            + (s.cooldown_sec != null && s.cooldown_sec > 0 ? "  •  CD " + s.cooldown_sec + "s" : "");

        // ---------- layout & wrapping ----------
        final int pad = padding.get();
        final int gutter = colGutter.get();
        final double lineH = r.textHeight() * (compact.get() ? 1.0 : 1.15);

        double innerMax = Math.max(220, maxWidth.get()) - pad * 2;
        double colW = Math.floor((innerMax - gutter) / 2.0);

        // 2-column blocks (wrap each piece to its column width)
        List<String> leftCol  = new ArrayList<>();
        List<String> rightCol = new ArrayList<>();
        leftCol.addAll(wrap(r, worldA, colW));
        leftCol.addAll(wrap(r, worldB, colW));
        rightCol.addAll(wrap(r, goalA, colW));
        rightCol.addAll(wrap(r, goalB, colW));

        int colRows = Math.max(leftCol.size(), rightCol.size());

        // single-column wrapped blocks
        List<String> body = new ArrayList<>();
        if (statusLine != null) body.addAll(wrap(r, statusLine, innerMax));
        if (goalExplLine != null) body.addAll(wrap(r, goalExplLine, innerMax));
        if (playerLine != null)   body.addAll(wrap(r, playerLine, innerMax));
        if (reasonLine != null)   body.addAll(wrap(r, reasonLine, innerMax));

        // total size
        double w = maxWidth.get();
        double h = pad + r.textHeight() + // header
            (statusLine != null ? lineH : 0) +
            colRows * lineH +
            (body.size() * lineH) +
            lineH + // age
            pad + (compact.get() ? 2 : 4);

        setSize(w, h);
        drawBackground(r, w, h);

        // ---------- draw ----------
        double xx = x + pad;
        double yy = y + pad;

        // Header
        r.text(header, xx, yy, stateColor, true);
        yy += r.textHeight();

        // Optional status line
        if (statusLine != null) {
            yy += compact.get() ? 0 : 2;
            r.text(statusLine, xx, yy, SUBTLE, true);
            yy += lineH;
        }

        // Two columns
        yy += compact.get() ? 0 : 2;
        double leftX = xx;
        double rightX = xx + colW + gutter;
        for (int i = 0; i < colRows; i++) {
            String L = i < leftCol.size()  ? leftCol.get(i)  : "";
            String R = i < rightCol.size() ? rightCol.get(i) : "";
            r.text(L, leftX,  yy, TEXT, true);
            r.text(R, rightX, yy, TEXT, true);
            yy += lineH;
        }

        // Body (single column)
        for (String line : body) {
            yy += compact.get() ? 0 : 2;
            Color c = (line.startsWith("Reason:")) ? REASON_COL : (line.startsWith("Last player:") ? TEXT : SUBTLE);
            r.text(line, xx, yy, c, true);
            yy += lineH;
        }

        // Age
        yy += compact.get() ? 0 : 2;
        r.text(age, xx, yy, SUBTLE, true);
    }

    // ---------- helpers ----------
    private void drawBackground(HudRenderer r, double w, double h) {
        if (!showBackground.get()) return;

        if (dropShadow.get()) {
            SettingColor sc = shadowColor.get();
            Color c = new Color(sc.r, sc.g, sc.b, sc.a);
            int off = shadowOffset.get();
            r.quad(x + off, y + off, w, h, c);
        }
        SettingColor sc = bgColor.get();
        r.quad(x, y, w, h, new Color(sc.r, sc.g, sc.b, sc.a));
    }

    private static String safe(String s, String d) { return (s == null || s.isBlank()) ? d : s; }

    private static String shortDim(String d) {
        d = safe(d, "overworld").toLowerCase(Locale.ROOT);
        if (d.contains("nether")) return "NETH";
        if (d.contains("end"))    return "END";
        return "OVW";
    }

    private static String fmtSpeed(Double v) {
        if (v == null || !Double.isFinite(v)) return "— b/s";
        return String.format(Locale.US, "%.2f b/s", v);
    }

    private static String fmtInt(Integer n) {
        if (n == null) return "—";
        long v = n.longValue();
        if (v >= 1_000_000) return String.format(Locale.US, "%.1fm", v / 1_000_000.0);
        if (v >= 10_000)    return String.format(Locale.US, "%.0fk", v / 1000.0);
        if (v >= 1_000)     return String.format(Locale.US, "%.1fk", v / 1000.0);
        return Long.toString(v);
    }

    private static double clamp(double max, double val) { return Math.min(max, val); }

    // Greedy word wrap that respects HudRenderer text metrics
    private static List<String> wrap(HudRenderer r, String text, double maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;

        String[] words = text.trim().split("\\s+");
        if (words.length == 0) return out;

        String line = words[0];
        for (int i = 1; i < words.length; i++) {
            String tryLine = line + " " + words[i];
            if (r.textWidth(tryLine) <= maxWidth) {
                line = tryLine;
            } else {
                out.add(ellipsisIfNeeded(r, line, maxWidth));
                line = words[i];
            }
        }
        out.add(ellipsisIfNeeded(r, line, maxWidth));
        return out;
    }

    private static String ellipsisIfNeeded(HudRenderer r, String s, double maxWidth) {
        if (r.textWidth(s) <= maxWidth) return s;
        String ell = "…";
        // binary chop to fit
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            String t = s.substring(0, mid) + ell;
            if (r.textWidth(t) <= maxWidth) lo = mid + 1;
            else hi = mid;
        }
        int cut = Math.max(0, lo - 1);
        return s.substring(0, cut) + ell;
    }
}
