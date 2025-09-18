package com.april777.auton8.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Docker containers directly from the Fabric mod
 * Replaces external docker-compose scripts with programmatic container management
 */
public class DockerManager {
    private DockerClient dockerClient;
    private String n8nContainerId;
    private final String projectDir;
    
    public DockerManager(String projectDir) {
        this.projectDir = projectDir;
        initializeDockerClient();
    }
    
    private void initializeDockerClient() {
        try {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock") // Linux default
                .build();
            
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
                
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
        } catch (Exception e) {
            System.err.println("Failed to initialize Docker client: " + e.getMessage());
        }
    }
    
    /**
     * Start n8n container with Minescript integration
     */
    public boolean startN8nContainer() {
        try {
            // Check if container already exists and is running
            if (isN8nContainerRunning()) {
                System.out.println("n8n container is already running");
                return true;
            }
            
            // Remove existing container if it exists but is stopped
            removeExistingN8nContainer();
            
            // Pull latest n8n image
            dockerClient.pullImageCmd("n8nio/n8n:latest").exec(null);
            
            // Create container with Minescript-compatible configuration
            Map<String, String> environment = createN8nEnvironment();
            
            CreateContainerResponse container = dockerClient.createContainerCmd("n8nio/n8n:latest")
                .withName("auton8-n8n")
                .withEnv(environment.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toArray(String[]::new))
                .withPortSpecs("5678:5678")
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(5678), ExposedPort.tcp(5678)))
                .withBinds(new Bind(projectDir + "/n8n/.n8n", new Volume("/home/node/.n8n")))
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
                .withNetworkMode("bridge")
                .exec();
                
            n8nContainerId = container.getId();
            
            // Start the container
            dockerClient.startContainerCmd(n8nContainerId).exec();
            
            System.out.println("n8n container started successfully with ID: " + n8nContainerId);
            
            // Wait for container to be fully ready
            waitForContainerReady();
            
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start n8n container: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create environment variables for n8n container with Minescript support
     */
    private Map<String, String> createN8nEnvironment() {
        Map<String, String> env = new HashMap<>();
        
        // Basic n8n configuration
        env.put("N8N_HOST", "localhost");
        env.put("N8N_PORT", "5678");
        env.put("N8N_PROTOCOL", "http");
        env.put("N8N_EDITOR_BASE_URL", "http://localhost:5678");
        env.put("GENERIC_TIMEZONE", "Africa/Johannesburg");
        
        // Database configuration
        env.put("DB_TYPE", "sqlite");
        env.put("DB_SQLITE_VACUUM_ON_STARTUP", "true");
        
        // Minescript API configuration
        env.put("N8N_CUSTOM_EXTENSIONS", "/home/node/.n8n/custom");
        env.put("MINESCRIPT_API_ENABLED", "true");
        env.put("MINESCRIPT_API_PORT", "5679");
        env.put("MINESCRIPT_AUTH_REQUIRED", "true");
        
        // Security settings
        env.put("N8N_SECURE_COOKIE", "false"); // Development mode
        env.put("N8N_LOG_LEVEL", "info");
        
        return env;
    }
    
    /**
     * Stop and remove n8n container
     */
    public boolean stopN8nContainer() {
        try {
            if (n8nContainerId != null) {
                dockerClient.stopContainerCmd(n8nContainerId).exec();
                dockerClient.removeContainerCmd(n8nContainerId).exec();
                n8nContainerId = null;
                System.out.println("n8n container stopped and removed");
                return true;
            }
            return true;
        } catch (Exception e) {
            System.err.println("Failed to stop n8n container: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if n8n container is running
     */
    public boolean isN8nContainerRunning() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(false)
                .withNameFilter(Collections.singletonList("auton8-n8n"))
                .exec();
            return !containers.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove existing n8n container if it exists
     */
    private void removeExistingN8nContainer() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(Collections.singletonList("auton8-n8n"))
                .exec();
                
            for (Container container : containers) {
                if (container.getState().equals("running")) {
                    dockerClient.stopContainerCmd(container.getId()).exec();
                }
                dockerClient.removeContainerCmd(container.getId()).exec();
            }
        } catch (Exception e) {
            // Ignore errors when removing non-existent containers
        }
    }
    
    /**
     * Wait for container to be fully ready
     */
    private void waitForContainerReady() {
        try {
            Thread.sleep(5000); // Wait 5 seconds for container startup
            // Could implement health check here if needed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get n8n container status information
     */
    public ContainerStatus getN8nStatus() {
        try {
            if (n8nContainerId == null) {
                return new ContainerStatus(false, "Not started", null);
            }
            
            Container container = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withIdFilter(Collections.singletonList(n8nContainerId))
                .exec()
                .stream()
                .findFirst()
                .orElse(null);
                
            if (container == null) {
                return new ContainerStatus(false, "Not found", null);
            }
            
            return new ContainerStatus(
                "running".equals(container.getState()),
                container.getState(),
                container.getStatus()
            );
        } catch (Exception e) {
            return new ContainerStatus(false, "Error", e.getMessage());
        }
    }
    
    /**
     * Cleanup Docker resources
     */
    public void cleanup() {
        try {
            stopN8nContainer();
            if (dockerClient != null) {
                dockerClient.close();
            }
        } catch (Exception e) {
            System.err.println("Error during Docker cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Container status information
     */
    public static class ContainerStatus {
        public final boolean isRunning;
        public final String state;
        public final String status;
        
        public ContainerStatus(boolean isRunning, String state, String status) {
            this.isRunning = isRunning;
            this.state = state;
            this.status = status;
        }
    }
}
