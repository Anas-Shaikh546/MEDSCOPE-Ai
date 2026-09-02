package com.medscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedscopeApplication {

    public static void main(String[] args) {
        // Load .env file if it exists
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(".env")) {
                    props.load(fis);
                    props.forEach((key, value) -> {
                        String keyStr = key.toString();
                        String valueStr = value.toString();
                        // Only set if not already set as system property or environment variable
                        if (System.getProperty(keyStr) == null && System.getenv(keyStr) == null) {
                            System.setProperty(keyStr, valueStr);
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }

        SpringApplication.run(MedscopeApplication.class, args);
    }
}
