package dev.markodojkic.singiattend.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests()  // Updated to use authorizeHttpRequests()
                .anyRequest().authenticated()  // Apply authentication to all requests
                .and()
                .httpBasic()  // Enable HTTP Basic authentication
                .authenticationEntryPoint(myAuthenticationEntryPoint());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder.inMemoryAuthentication()
                .withUser("singiattend-admin")
                .password(passwordEncoder().encode("singiattend-server2021"))
                .authorities("singiattend");

        return authenticationManagerBuilder.build();  // Build the AuthenticationManager
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MyBasicAuthenticationEntryPoint myAuthenticationEntryPoint() {
        return new MyBasicAuthenticationEntryPoint();
    }
}

