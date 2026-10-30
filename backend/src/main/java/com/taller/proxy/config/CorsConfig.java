package com.taller.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Orígenes permitidos por la variable de entorno CORS_ORIGINS (separados por coma). */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.origins:*}")
    private String origins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Retry-After");
    }
}
