package com.selfhealing.monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HealthMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthMonitorApplication.class, args);
        System.out.println("🩺 Health Monitor is starting...");
        System.out.println("📊 Ready to monitor services!");
    }
}

// Service information storage
class ServiceInfo {
    private String serviceName;
    private String host;
    private int port;
    private LocalDateTime lastHeartbeat;
    private String status;

    // Constructor
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
}

@RestController
@RequestMapping("/monitor")
class HealthMonitorController {
    
    // Storage for all registered services
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Endpoint to register a new service
    @PostMapping("/register")
    public Map<String, String> registerService(@RequestBody ServiceInfo serviceInfo) {
        serviceInfo.setLastHeartbeat(LocalDateTime.now());
        serviceInfo.setStatus("HEALTHY");
        services.put(serviceInfo.getServiceName(), serviceInfo);
        
        System.out.println("✅ Service registered: " + serviceInfo.getServiceName() + 
                          " on port " + serviceInfo.getPort());
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Service registered successfully");
        response.put("serviceName", serviceInfo.getServiceName());
        response.put("status", "HEALTHY");
        return response;
    }
    
    // Endpoint to receive heartbeat from services
    @PostMapping("/heartbeat")
    public Map<String, String> receiveHeartbeat(@RequestBody ServiceInfo serviceInfo) {
        ServiceInfo existing = services.get(serviceInfo.getServiceName());
        
        if (existing == null) {
            // Auto-register if not found
            serviceInfo.setStatus("HEALTHY");
            serviceInfo.setLastHeartbeat(LocalDateTime.now());
            services.put(serviceInfo.getServiceName(), serviceInfo);
            System.out.println("💚 Auto-registered: " + serviceInfo.getServiceName());
        } else {
            // Update existing service
            existing.setLastHeartbeat(LocalDateTime.now());
            existing.setStatus("HEALTHY");
            System.out.println("💓 Heartbeat from: " + serviceInfo.getServiceName() + 
                             " at " + LocalDateTime.now().format(formatter));
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Heartbeat received");
        response.put("serviceName", serviceInfo.getServiceName());
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
    
    // Endpoint to view all registered services
    @GetMapping("/services")
    public Map<String, Object> getAllServices() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalServices", services.size());
        response.put("services", services);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
    
    // Endpoint to check monitor health
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Health Monitor is running!");
        response.put("monitoringServices", String.valueOf(services.size()));
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
}