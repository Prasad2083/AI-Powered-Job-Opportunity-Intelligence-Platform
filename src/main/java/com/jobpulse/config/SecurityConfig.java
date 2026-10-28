package com.jobpulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import com.jobpulse.common.util.JwtTokenUtil;
import com.jobpulse.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * Security Configuration for CORS and password encoding.
 *
 * Configures Cross-Origin Resource Sharing (CORS) to allow requests from
 * front-end applications and sets up password encoding for user authentication.
 */
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenUtil tokens, UserRepository users) throws Exception {
        OncePerRequestFilter bearer = new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                String value = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (value != null && value.startsWith("Bearer ")) {
                    String token = value.substring(7);
                    if (tokens.validateToken(token)) {
                        String name = tokens.getUsernameFromToken(token);
                        users.findByUsername(name).filter(user -> user.isEnabled()).ifPresent(user ->
                            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name, null, List.of(new SimpleGrantedAuthority("ROLE_" + (user.getRole() == null ? "USER" : user.getRole()))))));
                    }
                }
                chain.doFilter(request, response);
            }
        };
        return http.csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/**", "/health").permitAll().anyRequest().authenticated())
            .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class).build();
    }


    /**
     * Configure CORS settings for the application.
     * Allows the frontend React application to communicate with the backend API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React dev server
            "http://localhost:5173",  // Vite dev server
            "https://jobpulse.com",
            "https://www.jobpulse.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configure password encoding using BCrypt algorithm.
     * Provides secure password hashing with adaptive work factor.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength factor 12
    }
}

