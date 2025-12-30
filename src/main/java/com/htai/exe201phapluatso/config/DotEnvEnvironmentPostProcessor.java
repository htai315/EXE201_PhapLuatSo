package com.htai.exe201phapluatso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads .env file into Spring Environment BEFORE application.properties is processed
 * This ensures environment variables are available for ${VAR} placeholders
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(DotEnvEnvironmentPostProcessor.class);
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envPath = Paths.get(".env");
        
        if (!Files.exists(envPath)) {
            log.warn("⚠️ .env file not found at: {}", envPath.toAbsolutePath());
            return;
        }
        
        log.info("📁 Loading .env file into Spring Environment: {}", envPath.toAbsolutePath());
        
        Map<String, Object> envProperties = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            int count = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse KEY=VALUE
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    envProperties.put(key, value);
                    count++;
                    
                    // Log without showing sensitive values
                    if (key.contains("SECRET") || key.contains("PASSWORD") || key.contains("KEY")) {
                        log.debug("✓ Loaded: {} = [HIDDEN]", key);
                    } else {
                        log.debug("✓ Loaded: {} = {}", key, value);
                    }
                }
            }
            
            // Add to Spring Environment with high priority
            environment.getPropertySources().addFirst(
                new MapPropertySource("dotenv", envProperties)
            );
            
            log.info("✅ Successfully loaded {} environment variables from .env into Spring Environment", count);
            
        } catch (IOException e) {
            log.error("❌ Error reading .env file: {}", e.getMessage());
        }
    }
}
