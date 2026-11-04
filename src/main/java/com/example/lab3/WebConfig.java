package com.example.lab3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path baseDir;

    public WebConfig(@Value("${media.base-path:media}") String basePath) throws IOException {
        this.baseDir = Path.of(basePath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir); // creat la startup dacă lipsește
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(baseDir.toUri().toString());
    }
}