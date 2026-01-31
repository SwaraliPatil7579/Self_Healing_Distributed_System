package com.selfhealing.serviceb;

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
@EnableScheduling
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
        System.out.println("🚀 Service B is starting...");
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
class HeartbeatService {
    
    private final RestTemplate restTemplate;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Value("${server.port}")
    private int port;
    
    @Value("${MONITOR_URL:http://localhost:8080/monitor/heartbeat}")
private String monitorUrl;
    
    public HeartbeatService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            Map<String, Object> heartbeatData = new HashMap<>();
            heartbeatData.put("serviceName", serviceName);
            heartbeatData.put("host", "localhost");
            heartbeatData.put("port", port);
            
            restTemplate.postForObject(monitorUrl, heartbeatData, String.class);
            System.out.println("💓 Heartbeat sent to Health Monitor");
            
        } catch (Exception e) {
            System.out.println("❌ Failed to send heartbeat: " + e.getMessage());
        }
    }
}

@RestController
class ServiceBController {
    
    @GetMapping("/health")
    public String health() {
        return "✅ Service B is healthy and running!";
    }
    
    @GetMapping("/data")
    public String getData() {
        return "📊 Here's some data from Service B - Timestamp: " + System.currentTimeMillis();
    }
}