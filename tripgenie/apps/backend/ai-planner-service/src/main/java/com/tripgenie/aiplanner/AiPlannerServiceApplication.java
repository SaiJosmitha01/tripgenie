package com.tripgenie.aiplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tripgenie")
public class AiPlannerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPlannerServiceApplication.class, args);
    }
}
