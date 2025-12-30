package com.htai.exe201phapluatso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class to load environment variables from .env file
 * This is a fallback solution when EnvFile plugin is not available
 */
public class EnvLoader {
    
    private static final Logger log = LoggerFactory.getLogger(EnvLoader.class);
    
    /**
     * Load .env file from project root
     * Call this BEFORE SpringApplication.run()
     */
    public static void loadEnv() {
        Path envPath = Paths.get(".env");
        
        if (!Files.exists(envPath)) {
            log.warn("⚠️ .env file not found at: {}", envPath.toAbsolutePath());
            log.warn("Using default values from application.properties");
            return;
        }
        
        log.info("📁 Loading environment variables from: {}", envPath.toAbsolutePath());
        
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
                    
                    // Set as system property (Spring Boot will pick it up)
                    System.setProperty(key, value);
                    count++;
                    
                    // Log without showing sensitive values
                    if (key.contains("SECRET") || key.contains("PASSWORD") || key.contains("KEY")) {
                        log.debug("✓ Loaded: {} = [HIDDEN]", key);
                    } else {
                        log.debug("✓ Loaded: {} = {}", key, value);
                    }
                }
            }
            
            log.info("✅ Successfully loaded {} environment variables from .env", count);
            
        } catch (IOException e) {
            log.error("❌ Error reading .env file: {}", e.getMessage());
        }
    }
}
