package com.selfhealing.monitor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;

/**
 * Manages Docker container operations.
 * This component can start, stop, and restart containers.
 */
@Component
public class DockerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DockerManager.class);
    
    private DockerClient dockerClient;
    
    /**
     * Initialize Docker client when component starts.
     * This runs automatically when Spring creates this bean.
     */
    @PostConstruct
    public void init() {
        try {
            // Configure Docker client
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost("unix:///var/run/docker.sock")
        .build();

DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .build();

dockerClient = DockerClientImpl.getInstance(config, httpClient);

            logger.info("✅ Docker Manager initialized successfully");
            
            // Test connection by listing containers
            testConnection();
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize Docker Manager: {}", e.getMessage());
            logger.error("Make sure Docker API is enabled (Settings -> Expose daemon on tcp://localhost:2375)");
        }
    }
    
    /*
     Test Docker connection by listing containers.
     */
    private void testConnection() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec();
            logger.info("📦 Found {} containers", containers.size());
            
            // Log container names for debugging
            for (Container container : containers) {
                String name = container.getNames()[0].replace("/", "");
                String status = container.getState();
                logger.info("  - {} ({})", name, status);
            }
            
        } catch (Exception e) {
            logger.error("❌ Docker connection test failed: {}", e.getMessage());
        }
    }
    
    /**
     * Find container ID by service name.
     * 
     * @param serviceName Name of the service (e.g., "service-a")
     * @return Container ID if found, null otherwise
     */
    public String findContainerIdByName(String serviceName) {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true) // Include stopped containers
                    .exec();
            
            for (Container container : containers) {
                // Container names start with "/" so we need to clean them
                String containerName = container.getNames()[0].replace("/", "");
                
                // Check if name contains service name
                // Docker Compose names are like: "project_service-a_1"
                if (containerName.contains(serviceName)) {
                    logger.debug("Found container {} for service {}", container.getId(), serviceName);
                    return container.getId();
                }
            }
            
            logger.warn("Container not found for service: {}", serviceName);
            return null;
            
        } catch (Exception e) {
            logger.error("Error finding container for service {}: {}", serviceName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Restart a container by service name.
     * This is the MAIN self-healing action!
     * 
     * @param serviceName Name of the service to restart
     * @return true if restart was successful, false otherwise
     */
    public boolean restartContainer(String serviceName) {
        try {
            // Step 1: Find container ID
            String containerId = findContainerIdByName(serviceName);
            
            if (containerId == null) {
                logger.error("❌ Cannot restart {}: Container not found", serviceName);
                return false;
            }
            
            // Step 2: Check current state
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            Boolean isRunning = containerInfo.getState().getRunning();
            
            logger.info("🔄 Attempting to restart {} (ID: {}, Running: {})", 
                    serviceName, containerId.substring(0, 12), isRunning);
            
            // Step 3: Restart the container
            dockerClient.restartContainerCmd(containerId)
                    .withTimeout(10) // Wait max 10 seconds
                    .exec();
            
            // Step 4: Verify it's running
            Thread.sleep(2000); // Wait 2 seconds for container to start
            
            containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            isRunning = containerInfo.getState().getRunning();
            
            if (isRunning) {
                logger.info("✅ Successfully restarted {} - Container is now RUNNING", serviceName);
                return true;
            } else {
                logger.error("❌ Restart command executed but {} is not running", serviceName);
                return false;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ Restart interrupted for {}: {}", serviceName, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ Failed to restart {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Start a stopped container.
     * 
     * @param serviceName Name of the service to start
     * @return true if start was successful
     */
    public boolean startContainer(String serviceName) {
        try {
            String containerId = findContainerIdByName(serviceName);
            
            if (containerId == null) {
                logger.error("❌ Cannot start {}: Container not found", serviceName);
                return false;
            }
            
            logger.info("▶️ Starting container for {}", serviceName);
            dockerClient.startContainerCmd(containerId).exec();
            
            logger.info("✅ Started container for {}", serviceName);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to start {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop a running container.
     * 
     * @param serviceName Name of the service to stop
     * @return true if stop was successful
     */
    public boolean stopContainer(String serviceName) {
        try {
            String containerId = findContainerIdByName(serviceName);
            
            if (containerId == null) {
                logger.error("❌ Cannot stop {}: Container not found", serviceName);
                return false;
            }
            
            logger.info("⏸️ Stopping container for {}", serviceName);
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(10)
                    .exec();
            
            logger.info("✅ Stopped container for {}", serviceName);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to stop {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get container status.
     * 
     * @param serviceName Name of the service
     * @return "running", "exited", "not_found", or "error"
     */
    public String getContainerStatus(String serviceName) {
        try {
            String containerId = findContainerIdByName(serviceName);
            
            if (containerId == null) {
                return "not_found";
            }
            
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            return containerInfo.getState().getStatus();
            
        } catch (Exception e) {
            logger.error("Error getting status for {}: {}", serviceName, e.getMessage());
            return "error";
        }
    }
    
    /**
     * Cleanup when component is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        try {
            if (dockerClient != null) {
                dockerClient.close();
                logger.info("Docker Manager closed successfully");
            }
        } catch (Exception e) {
            logger.error("Error closing Docker Manager: {}", e.getMessage());
        }
    }
}