package com.rocha.spacecraftmanagementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://localhost:5174",
                                "http://localhost:5175",
                                "http://localhost:5176",
                                "https://spacecraft-system.web.app",
                                "https://spacecraft-system.firebaseapp.com",
                                "https://spacecraft-tickets.web.app",
                                "https://spacecraft-tickets.firebaseapp.com",
                                "https://spacecraft-taller.web.app",
                                "https://spacecraft-taller.firebaseapp.com",
                                "https://spacecraft-events.web.app",
                                "https://spacecraft-events.firebaseapp.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}