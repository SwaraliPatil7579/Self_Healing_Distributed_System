package com.selfhealing.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
public class HealthMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthMonitorApplication.class, args);
        System.out.println("🩺 Health Monitor is starting...");
        System.out.println("📊 Ready to monitor services!");
        System.out.println("🔍 Failure detection enabled!");
    }
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}

// Service information storage
class ServiceInfo {
    private String serviceName;
    private String host;
    private int port;
    private LocalDateTime lastHeartbeat;
    private String status;
    private LocalDateTime lastStatusChange;

    public ServiceInfo() {}

    // Getters and Setters
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastStatusChange() { return lastStatusChange; }
    public void setLastStatusChange(LocalDateTime lastStatusChange) { this.lastStatusChange = lastStatusChange; }
}

// Failure Detection Component - THE BRAIN!
@Component
class FailureDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);
    private static final long FAILURE_THRESHOLD_SECONDS = 15;
    
    @Autowired
    private HealthMonitorController controller;

    
    @Autowired
    private DockerManager dockerManager; // 👈 INJECT Docker Manager
    
    /**
     * Check for failed services every 10 seconds.
     * Now with AUTOMATIC RESTART! 🚀
     */
    @Scheduled(fixedRate = 10000)
    public void detectFailures() {
        logger.debug("🔍 Running failure detection...");
        
        Map<String, ServiceInfo> services = controller.getServices();
        LocalDateTime now = LocalDateTime.now();
        
        for (ServiceInfo service : services.values()) {
            long secondsSinceHeartbeat = ChronoUnit.SECONDS.between(service.getLastHeartbeat(), now);
            
            boolean isDead = secondsSinceHeartbeat >= FAILURE_THRESHOLD_SECONDS;
            String previousStatus = service.getStatus();
            
            if (isDead && !"DEAD".equals(previousStatus)) {
                // SERVICE JUST DIED! 💀
                service.setStatus("DEAD");
                logger.error("💀 SERVICE FAILURE DETECTED: {} (no heartbeat for {} seconds)", 
                        service.getServiceName(), secondsSinceHeartbeat);
                
                // 🚀 AUTOMATIC RESTART! This is the magic!
                logger.warn("🔧 Initiating automatic recovery for {}...", service.getServiceName());
                attemptAutoRestart(service.getServiceName());
                
            } else if (!isDead && "DEAD".equals(previousStatus)) {
                // SERVICE RECOVERED! ✅
                service.setStatus("HEALTHY");
                logger.info("✅ SERVICE RECOVERED: {} (heartbeat received after {} seconds)", 
                        service.getServiceName(), secondsSinceHeartbeat);
            }
        }
    }
    
    /**
     * Attempt to automatically restart a failed service.
     * This makes the system truly self-healing!
     * 
     * @param serviceName Name of the service to restart
     */
    private void attemptAutoRestart(String serviceName) {
        try {
            logger.info("🔄 Attempting automatic restart of {}...", serviceName);
            
            // Call Docker Manager to restart container
            boolean success = dockerManager.restartContainer(serviceName);
            
            if (success) {
                logger.info("✅ AUTO-HEAL SUCCESS: {} container restarted!", serviceName);
                logger.info("⏳ Waiting for {} to send heartbeat...", serviceName);
            } else {
                logger.error("❌ AUTO-HEAL FAILED: Could not restart {} container", serviceName);
                logger.error("💡 Manual intervention may be required for {}", serviceName);
            }
            
        } catch (Exception e) {
            logger.error("❌ Exception during auto-restart of {}: {}", serviceName, e.getMessage());
        }
    }
}

@RestController
@RequestMapping("/monitor")
@CrossOrigin(origins = "*")
class HealthMonitorController {
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Expose services map for FailureDetector
    public Map<String, ServiceInfo> getServices() {
        return services;
    }
    
    @PostMapping("/register")
    public Map<String, String> registerService(@RequestBody ServiceInfo serviceInfo) {
        serviceInfo.setLastHeartbeat(LocalDateTime.now());
        serviceInfo.setStatus("HEALTHY");
        serviceInfo.setLastStatusChange(LocalDateTime.now());
        services.put(serviceInfo.getServiceName(), serviceInfo);
        
        System.out.println("✅ Service registered: " + serviceInfo.getServiceName() + 
                          " on port " + serviceInfo.getPort());
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Service registered successfully");
        response.put("serviceName", serviceInfo.getServiceName());
        response.put("status", "HEALTHY");
        return response;
    }
    
    @PostMapping("/heartbeat")
    public Map<String, String> receiveHeartbeat(@RequestBody ServiceInfo serviceInfo) {
        ServiceInfo existing = services.get(serviceInfo.getServiceName());
        
        if (existing == null) {
            // Auto-register
            serviceInfo.setStatus("HEALTHY");
            serviceInfo.setLastHeartbeat(LocalDateTime.now());
            serviceInfo.setLastStatusChange(LocalDateTime.now());
            services.put(serviceInfo.getServiceName(), serviceInfo);
            System.out.println("💚 Auto-registered: " + serviceInfo.getServiceName());
        } else {
            // Update existing service
            String previousStatus = existing.getStatus();
            existing.setLastHeartbeat(LocalDateTime.now());
            
            // If service was dead and now sending heartbeat, mark as recovered
            if ("DEAD".equals(previousStatus)) {
                existing.setStatus("HEALTHY");
                existing.setLastStatusChange(LocalDateTime.now());
                System.out.println("✅ RECOVERY: " + serviceInfo.getServiceName() + 
                                 " recovered and sending heartbeats again!");
            } else {
                existing.setStatus("HEALTHY");
            }
            
            System.out.println("💓 Heartbeat from: " + serviceInfo.getServiceName() + 
                             " at " + LocalDateTime.now().format(formatter));
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Heartbeat received");
        response.put("serviceName", serviceInfo.getServiceName());
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
    
    @GetMapping("/services")
    public Map<String, Object> getAllServices() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalServices", services.size());
        response.put("services", services);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        long healthyCount = services.values().stream()
            .filter(s -> "HEALTHY".equals(s.getStatus()))
            .count();
        long deadCount = services.values().stream()
            .filter(s -> "DEAD".equals(s.getStatus()))
            .count();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Health Monitor is running!");
        response.put("monitoringServices", String.valueOf(services.size()));
        response.put("healthyServices", String.valueOf(healthyCount));
        response.put("deadServices", String.valueOf(deadCount));
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
}