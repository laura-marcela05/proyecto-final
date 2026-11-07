package com.corhuila.gymtracker.security;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Route rules of authorization.md: one line per endpoint, so a reviewer can compare them with the table. */
@Configuration
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                           RestAuthenticationEntryPoint entryPoint,
                                           RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(POST, API + "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(GET, API + "/users").hasRole("ADMIN")
                        .requestMatchers(POST, API + "/users").hasRole("ADMIN")
                        .requestMatchers(GET, API + "/users/{userId}").hasRole("ADMIN")
                        .requestMatchers(PUT, API + "/users/{userId}").hasRole("ADMIN")
                        .requestMatchers(DELETE, API + "/users/{userId}").hasRole("ADMIN")
                        .requestMatchers(GET, API + "/students").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(POST, API + "/students").hasRole("TRAINER")
                        .requestMatchers(GET, API + "/students/{studentId}").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(PUT, API + "/students/{studentId}").hasRole("TRAINER")
                        .requestMatchers(DELETE, API + "/students/{studentId}").hasRole("ADMIN")
                        .requestMatchers(POST, API + "/students/{studentId}/sessions").hasRole("TRAINER")
                        .requestMatchers(GET, API + "/students/{studentId}/sessions").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(PUT, API + "/sessions/{sessionId}/muscle-groups/{muscleGroup}").hasRole("TRAINER")
                        .requestMatchers(PATCH, API + "/sessions/{sessionId}").hasRole("TRAINER")
                        .requestMatchers(DELETE, API + "/sessions/{sessionId}").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
