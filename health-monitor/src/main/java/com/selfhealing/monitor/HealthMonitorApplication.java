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
    
    private final Map<String, ServiceInfo> services;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long FAILURE_THRESHOLD_SECONDS = 15;
    
    public FailureDetector(HealthMonitorController controller) {
        this.services = controller.getServices();
    }
    
    // This runs automatically every 10 seconds!
    @Scheduled(fixedRate = 10000)
    public void detectFailures() {
        if (services.isEmpty()) {
            return; // No services to check
        }
        
        LocalDateTime now = LocalDateTime.now();
        System.out.println("\n🔍 [" + now.format(formatter) + "] Running failure detection...");
        
        for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            ServiceInfo info = entry.getValue();
            
            // Calculate seconds since last heartbeat
            long secondsSinceHeartbeat = ChronoUnit.SECONDS.between(
                info.getLastHeartbeat(), 
                now
            );
            
            String previousStatus = info.getStatus();
            String newStatus;
            
            // Determine new status based on time
            if (secondsSinceHeartbeat >= FAILURE_THRESHOLD_SECONDS) {
                newStatus = "DEAD";
            } else {
                newStatus = "HEALTHY";
            }
            
            // If status changed, log it and update!
            if (!newStatus.equals(previousStatus)) {
                info.setStatus(newStatus);
                info.setLastStatusChange(now);
                
                if (newStatus.equals("DEAD")) {
                    System.out.println("❌ FAILURE DETECTED: " + serviceName + 
                                     " is DEAD! Last heartbeat: " + secondsSinceHeartbeat + "s ago");
                } else {
                    System.out.println("✅ RECOVERY DETECTED: " + serviceName + 
                                     " is back HEALTHY!");
                }
            }
            
            // Always show current status
            String statusIcon = newStatus.equals("HEALTHY") ? "💚" : "💀";
            System.out.println("   " + statusIcon + " " + serviceName + 
                             " - Status: " + newStatus + 
                             " (Last heartbeat: " + secondsSinceHeartbeat + "s ago)");
        }
        
        System.out.println("🔍 Failure detection complete.\n");
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