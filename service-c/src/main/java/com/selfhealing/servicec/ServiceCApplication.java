package com.selfhealing.servicec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ServiceCApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceCApplication.class, args);
        System.out.println("🚀 Service C is starting...");
    }
}

@RestController
class ServiceCController {
    
    @GetMapping("/health")
    public String health() {
        return "✅ Service C is healthy and running!";
    }
    
    @GetMapping("/data")
    public String getData() {
        return "📊 Here's some data from Service C!";
    }
}