package com.example.lab3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless cu Basic auth
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/media").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                .anyRequest().permitAll()) // pagina, GET /api/posts, GET /media/**
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}