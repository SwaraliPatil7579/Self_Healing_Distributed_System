package com.selfhealing.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
        System.out.println("🚀 Service B is starting...");
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
        return "📊 Here's some data from Service B!";
    }
}