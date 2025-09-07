package com.kilab.auton8.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.util.Util;
import java.net.URI;

import net.minecraft.client.MinecraftClient;


import static com.kilab.auton8.Auton8.CAT;   // reuse registered category


public class OpenWebModule extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> host = sg.add(new StringSetting.Builder()
        .name("host").defaultValue("127.0.0.1").build());
    private final Setting<Integer> port = sg.add(new IntSetting.Builder()
        .name("port").defaultValue(5678).min(1).max(65535).build());
    private final Setting<Boolean> https = sg.add(new BoolSetting.Builder()
        .name("https").defaultValue(false).build());
    private final Setting<String> path = sg.add(new StringSetting.Builder()
        .name("path").defaultValue("").build());

    // simple debounce so stray clicks can’t retrigger it
    private static long lastOpenMs = 0;

    public OpenWebModule() {
        super(CAT, "open-n8n", "Opens your n8n UI in the system browser.");
    }

    @Override
    public void onActivate() {
        long now = System.currentTimeMillis();
        if (now - lastOpenMs < 800) { toggle(); return; }  // debounce ~0.8s
        lastOpenMs = now;

        // close ClickGUI so the “click to refocus” doesn’t hit the module again
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) mc.setScreen(null);

        String p = path.get().isEmpty() ? "" : (path.get().startsWith("/") ? path.get() : "/" + path.get());
        String url = (https.get() ? "https://" : "http://") + host.get() + ":" + port.get() + p;

        open(url);
        info("Opening n8n: " + safe(url));
        toggle(); // auto-disable after opening
    }

    private void open(String url) {
        try { Util.getOperatingSystem().open(url); }
        catch (Throwable t) { try { java.awt.Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {} }
    }

    private String safe(String url) {
        int at = url.indexOf('@'), scheme = url.indexOf("://");
        return (at > scheme && scheme >= 0) ? url.substring(0, scheme + 3) + "<redacted>@" + url.substring(at + 1) : url;
    }
}
