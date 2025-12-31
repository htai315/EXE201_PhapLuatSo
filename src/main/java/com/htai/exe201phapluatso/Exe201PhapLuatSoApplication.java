package com.htai.exe201phapluatso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class Exe201PhapLuatSoApplication {

    public static void main(String[] args) {
        // Load .env file BEFORE Spring Boot starts
        SpringApplication app = new SpringApplication(Exe201PhapLuatSoApplication.class);
        
        // Add custom property source from .env file
        app.addInitializers(context -> {
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                System.out.println("📁 Loading .env file: " + envPath.toAbsolutePath());
                Map<String, Object> envProperties = new HashMap<>();
                
                try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        
                        int equalsIndex = line.indexOf('=');
                        if (equalsIndex > 0) {
                            String key = line.substring(0, equalsIndex).trim();
                            String value = line.substring(equalsIndex + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            envProperties.put(key, value);
                        }
                    }
                    
                    context.getEnvironment().getPropertySources().addFirst(
                        new MapPropertySource("dotenv", envProperties)
                    );
                    System.out.println("✅ Loaded " + envProperties.size() + " variables from .env");
                } catch (Exception e) {
                    System.err.println("❌ Error loading .env: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ .env file not found");
            }
        });
        
        app.run(args);
    }

}
