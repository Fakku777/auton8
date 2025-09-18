package com.april777.auton8.modules;

import com.april777.auton8.Auton8;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.client.MinecraftClient;

public class AuthConfigModule extends Module {
    private final SettingGroup sgConnection = settings.createGroup("Connection");
    private final SettingGroup sgAuth = settings.createGroup("Authentication");
    private final SettingGroup sgEndpoints = settings.createGroup("Endpoints");

    // Connection settings
    public final Setting<String> minescriptUrl = sgConnection.add(new StringSetting.Builder()
        .name("minescript-url")
        .description("Base URL for Minescript API")
        .defaultValue("http://127.0.0.1:5679/api")
        .build());

    // Authentication settings
    public final Setting<String> clientId = sgAuth.add(new StringSetting.Builder()
        .name("client-id")
        .description("Client ID for authentication with n8n")
        .defaultValue("777april-client")
        .build());

    public final Setting<String> authKey = sgAuth.add(new StringSetting.Builder()
        .name("auth-key")
        .description("Authentication key for n8n access")
        .defaultValue("your-super-strong-auth-key")
        .build());

    // Endpoint settings
    public final Setting<String> cmdEndpoint = sgEndpoints.add(new StringSetting.Builder()
        .name("command-endpoint")
        .description("Endpoint for receiving commands from n8n")
        .defaultValue("cmd")
        .build());

    public final Setting<String> evtEndpoint = sgEndpoints.add(new StringSetting.Builder()
        .name("event-endpoint")
        .description("Endpoint for publishing events to n8n")
        .defaultValue("events")
        .build());

    public final Setting<String> hudEndpoint = sgEndpoints.add(new StringSetting.Builder()
        .name("hud-endpoint")
        .description("Endpoint for HUD updates")
        .defaultValue("hud")
        .build());

    public final Setting<String> stateEndpoint = sgEndpoints.add(new StringSetting.Builder()
        .name("baritone-state-endpoint")
        .description("Endpoint for Baritone state snapshots")
        .defaultValue("baritone_state")
        .build());

    // Docker settings
    private final SettingGroup sgDocker = settings.createGroup("Docker");
    
    public final Setting<Boolean> autoStartDocker = sgDocker.add(new BoolSetting.Builder()
        .name("auto-start-docker")
        .description("Automatically start n8n Docker container when module activates")
        .defaultValue(true)
        .build());

    public final Setting<String> dockerProjectDir = sgDocker.add(new StringSetting.Builder()
        .name("docker-project-dir")
        .description("Directory containing docker-compose.yml for n8n")
        .defaultValue(System.getProperty("user.dir"))
        .build());

    public AuthConfigModule() {
        super(Auton8.CAT, "minescript-auth", "Configure authentication and endpoints for Minescript API connection to n8n.");
    }

    @Override
    public void onActivate() {
        info("Auth configuration updated. Restart the n8n connection module to apply changes.");
        toggle(); // Auto-disable after showing settings
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        
        // Add a button to test the connection
        WButton testBtn = table.add(theme.button("Test Connection")).expandX().widget();
        testBtn.action = () -> {
            info("Testing connection to: " + minescriptUrl.get());
            // TODO: Add actual connection test logic here
            info("Connection test completed. Check logs for details.");
        };
        table.row();
        
        // Add a button to generate new credentials
        WButton generateBtn = table.add(theme.button("Generate New Auth Key")).expandX().widget();
        generateBtn.action = () -> {
            String newAuthKey = java.util.UUID.randomUUID().toString().replace("-", "");
            authKey.set(newAuthKey);
            info("Generated new auth key: " + newAuthKey);
        };
        table.row();
        
        // JSON Generation section
        table.add(theme.label("─── JSON Auth Export ───")).expandX();
        table.row();
        
        // Add a button to copy pretty JSON credentials to clipboard
        WButton copyJsonBtn = table.add(theme.button("Copy Pretty JSON")).expandX().widget();
        copyJsonBtn.action = () -> {
            try {
                String json = generateAuthJson();
                copyToClipboard(json);
                info("Pretty Authentication JSON copied to clipboard!");
                info("Paste this into your Custom Auth account credential field in n8n.");
            } catch (Exception e) {
                error("Failed to copy to clipboard: " + e.getMessage());
            }
        };
        table.row();
        
        // Add a button to copy compact JSON
        WButton copyCompactBtn = table.add(theme.button("Copy Compact JSON")).expandX().widget();
        copyCompactBtn.action = () -> {
            try {
                String json = generateCompactAuthJson();
                copyToClipboard(json);
                info("Compact Authentication JSON copied to clipboard!");
                info("Use this for single-line configurations or URL parameters.");
            } catch (Exception e) {
                error("Failed to copy to clipboard: " + e.getMessage());
            }
        };
        table.row();
        
        // Add a button to save JSON to file
        WButton saveJsonBtn = table.add(theme.button("Save JSON to File")).expandX().widget();
        saveJsonBtn.action = () -> {
            try {
                String json = generateAuthJson();
                saveJsonToFile(json);
                info("Authentication JSON saved to auton8_auth.json in Minecraft directory!");
            } catch (Exception e) {
                error("Failed to save JSON file: " + e.getMessage());
            }
        };
        table.row();
        
        // Add a button to view the generated JSON
        WButton viewJsonBtn = table.add(theme.button("View Auth JSON")).expandX().widget();
        viewJsonBtn.action = () -> {
            String json = generateAuthJson();
            info("Generated Authentication JSON:");
            info(json);
        };
        table.row();
        
        // Add current session info
        table.add(theme.label("Current Session")).expandX();
        table.row();
        table.add(theme.label("Ready to configure auth settings")).expandX();
        
        return table;
    }

    // Helper methods to get current values (for use by other modules)
    public String getMinescriptUrl() { return minescriptUrl.get(); }
    public String getClientId() { return clientId.get(); }
    public String getAuthKey() { return authKey.get(); }
    public String getCmdEndpoint() { return cmdEndpoint.get(); }
    public String getEvtEndpoint() { return evtEndpoint.get(); }
    public String getHudEndpoint() { return hudEndpoint.get(); }
    public String getStateEndpoint() { return stateEndpoint.get(); }
    public boolean getAutoStartDocker() { return autoStartDocker.get(); }
    public String getDockerProjectDir() { return dockerProjectDir.get(); }
    
    /**
     * Copies text to clipboard using multiple fallback methods
     */
    private void copyToClipboard(String text) {
        try {
            // Method 1: Try Minecraft's built-in clipboard if available
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.keyboard != null) {
                client.keyboard.setClipboard(text);
                return;
            }
        } catch (Exception e) {
            // Fall through to next method
        }
        
        try {
            // Method 2: Try AWT Toolkit (may fail in headless environments)
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
            return;
        } catch (Exception e) {
            // Fall through to next method
        }
        
        try {
            // Method 3: Try using ProcessBuilder to call system clipboard
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("linux")) {
                // Try xclip first, then xsel as fallback
                try {
                    pb = new ProcessBuilder("xclip", "-selection", "clipboard");
                    Process process = pb.start();
                    process.getOutputStream().write(text.getBytes());
                    process.getOutputStream().close();
                    process.waitFor();
                    return;
                } catch (Exception e1) {
                    try {
                        pb = new ProcessBuilder("xsel", "--clipboard", "--input");
                        Process process = pb.start();
                        process.getOutputStream().write(text.getBytes());
                        process.getOutputStream().close();
                        process.waitFor();
                        return;
                    } catch (Exception e2) {
                        // Continue to final fallback
                    }
                }
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("pbcopy");
                Process process = pb.start();
                process.getOutputStream().write(text.getBytes());
                process.getOutputStream().close();
                process.waitFor();
                return;
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "echo " + text.replace("\\", "\\\\").replace("^", "^^") + " | clip");
                pb.start().waitFor();
                return;
            }
        } catch (Exception e) {
            // All methods failed
        }
        
        // If all clipboard methods fail, show the JSON in chat as fallback
        info("Clipboard access failed. Here's your JSON (select and copy manually):");
        info(text);
        throw new RuntimeException("All clipboard methods failed. JSON displayed in chat.");
    }
    
    /**
     * Saves JSON to a file in the Minecraft directory
     */
    private void saveJsonToFile(String json) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        Path minecraftDir = client.runDirectory.toPath();
        Path jsonFile = minecraftDir.resolve("auton8_auth.json");
        
        try (FileWriter writer = new FileWriter(jsonFile.toFile())) {
            writer.write(json);
        }
        
        info("JSON saved to: " + jsonFile.toString());
    }
    
    /**
     * Generates a JSON object containing all authentication and connection information
     * that can be pasted into n8n's Custom Auth credential field
     */
    public String generateAuthJson() {
        Map<String, Object> authData = new HashMap<>();
        
        // Connection information
        authData.put("baseUrl", getMinescriptUrl());
        authData.put("clientId", getClientId());
        authData.put("authKey", getAuthKey());
        
        // API endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("command", getCmdEndpoint());
        endpoints.put("events", getEvtEndpoint());
        endpoints.put("hud", getHudEndpoint());
        endpoints.put("baritoneState", getStateEndpoint());
        authData.put("endpoints", endpoints);
        
        // Docker configuration
        Map<String, Object> docker = new HashMap<>();
        docker.put("autoStart", getAutoStartDocker());
        docker.put("projectDir", getDockerProjectDir());
        authData.put("docker", docker);
        
        // Meta information
        authData.put("generatedAt", java.time.Instant.now().toString());
        authData.put("version", "1.0");
        authData.put("description", "Auton8 Minescript API Authentication");
        
        // Create pretty-printed JSON
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
            
        return gson.toJson(authData);
    }
    
    /**
     * Generates a compact JSON suitable for URL parameters or single-line usage
     */
    public String generateCompactAuthJson() {
        Map<String, Object> authData = new HashMap<>();
        authData.put("baseUrl", getMinescriptUrl());
        authData.put("clientId", getClientId());
        authData.put("authKey", getAuthKey());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("cmd", getCmdEndpoint());
        endpoints.put("evt", getEvtEndpoint());
        endpoints.put("hud", getHudEndpoint());
        endpoints.put("state", getStateEndpoint());
        authData.put("endpoints", endpoints);
        
        Gson gson = new Gson();
        return gson.toJson(authData);
    }
}
