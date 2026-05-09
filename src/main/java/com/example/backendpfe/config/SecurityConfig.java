package com.example.backendpfe.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/fraud-test/**").permitAll()

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
// NOTIFICATIONS
                                .requestMatchers(HttpMethod.POST, "/api/notifications")
                                .hasAnyRole("SUPER_ADMIN", "ANALYST", "AUDITOR")

                                .requestMatchers(HttpMethod.GET, "/api/notifications/my/unread-count")
                                .authenticated()

                                .requestMatchers(HttpMethod.GET, "/api/notifications/my")
                                .authenticated()

                                .requestMatchers(HttpMethod.GET, "/api/notifications/my/*")
                                .authenticated()

                                .requestMatchers(HttpMethod.PATCH, "/api/notifications/seen-all")
                                .authenticated()

                                .requestMatchers(HttpMethod.PATCH, "/api/notifications/*/seen")
                                .authenticated()

                        // CLIENT
                        .requestMatchers(HttpMethod.POST, "/api/transfers").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/transfers/my").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/account-requests/client/**").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/account-requests/client/**").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/accounts/my-accounts/**").hasRole("CLIENT")

                        // ANALYST
                        .requestMatchers(HttpMethod.GET, "/api/transfers/pending").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/transfers/*/approve").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/transfers/*/reject").hasRole("ANALYST")

                        // ACCOUNTS
                        .requestMatchers(HttpMethod.GET, "/api/accounts/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accounts").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/accounts/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/accounts/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/accounts/**").hasRole("SUPER_ADMIN")

                        // TRANSACTIONS
                        .requestMatchers(HttpMethod.GET, "/api/transactions/my").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/transactions/me")
                        .hasAnyRole("CLIENT", "ANALYST", "AUDITOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/transactions/me-test")
                        .hasAnyRole("CLIENT", "ANALYST", "AUDITOR", "SUPER_ADMIN")

                        // TRANSACTIONS BACKOFFICE
                        .requestMatchers(HttpMethod.GET, "/api/transactions/search")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.GET, "/api/transactions/account/**")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.GET, "/api/transactions/user/**")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.GET, "/api/transactions/*")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.GET, "/api/transactions")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.POST, "/api/transactions")
                        .hasAnyRole("SUPER_ADMIN", "ANALYST")

                        .requestMatchers(HttpMethod.GET, "/api/alerts/**")
                        .hasAnyRole("ANALYST", "AUDITOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/alerts/*/status")
                        .hasAnyRole("ANALYST", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cases/from-alert/**")
                        .hasAnyRole("ANALYST", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/cases/**")
                        .hasAnyRole("ANALYST", "AUDITOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cases/*/resolve")
                        .hasAnyRole("ANALYST", "SUPER_ADMIN")
                        .requestMatchers("/api/audit-logs/**")
                        .hasAnyRole("AUDITOR", "SUPER_ADMIN")
                        .requestMatchers("/api/reports/**")
                        .hasAnyRole("AUDITOR", "SUPER_ADMIN")
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/api/transfers/*/approve-blocked")
                                .hasRole("ANALYST")
                        .anyRequest().authenticated()


                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}