package com.nikoladesnica.mastermind.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",           // Vite dev (if you bypass proxy)
                        "https://mastermind-game.lovable.app",    // replace with actual hosted UI origin
                        "https://mastermind.nikoladesnica.com"
                )
                .allowedMethods("GET","POST","OPTIONS")
                .allowedHeaders("*");
    }
}
