package com.medibook.schedule.config;

import com.medibook.schedule.security.AuthTokenAuthenticationFilter;
import com.medibook.schedule.security.InternalServiceAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
            AuthTokenAuthenticationFilter authTokenAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/v1/schedules/internal/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/schedules/providers/**", "/api/v1/schedules/slots/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
