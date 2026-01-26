package com.selfhealing.servicea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableScheduling  // This enables scheduled tasks!
public class ServiceAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
        System.out.println("🚀 Service A is starting...");
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// Heartbeat Service - sends heartbeats automatically
@Service
class HeartbeatService {
    
    private final RestTemplate restTemplate;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Value("${server.port}")
    private int port;
    
    private static final String MONITOR_URL = "http://localhost:8080/monitor/heartbeat";
    
    public HeartbeatService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // This method runs automatically every 5 seconds!
    @Scheduled(fixedRate = 5000)  // 5000 milliseconds = 5 seconds
    public void sendHeartbeat() {
        try {
            Map<String, Object> heartbeatData = new HashMap<>();
            heartbeatData.put("serviceName", serviceName);
            heartbeatData.put("host", "localhost");
            heartbeatData.put("port", port);
            
            restTemplate.postForObject(MONITOR_URL, heartbeatData, String.class);
            System.out.println("💓 Heartbeat sent to Health Monitor");
            
        } catch (Exception e) {
            System.out.println("❌ Failed to send heartbeat: " + e.getMessage());
        }
    }
}

@RestController
class ServiceAController {
    
    @GetMapping("/health")
    public String health() {
        return "✅ Service A is healthy and running!";
    }
    
    @GetMapping("/data")
    public String getData() {
        return "📊 Here's some data from Service A - Timestamp: " + System.currentTimeMillis();
    }
}