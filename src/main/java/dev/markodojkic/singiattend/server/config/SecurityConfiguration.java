package dev.markodojkic.singiattend.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${server.ssl.key-password}")
    private String serverPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieHttpOnly(true);
        csrfTokenRepository.setCookiePath("/_csrf");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");

        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .httpBasic().authenticationEntryPoint(myAuthenticationEntryPoint()).and()
                .sessionManagement()
                .sessionFixation().migrateSession()
                .maximumSessions(1).maxSessionsPreventsLogin(true).and().and()
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .cors(Customizer.withDefaults())
                .requiresChannel().anyRequest().requiresSecure().and()
                .headers().xssProtection().and()
                .contentSecurityPolicy("default-src 'self'; script-src 'self'; object-src 'none'; style-src 'self';").and()
                .frameOptions().sameOrigin().and().build();

    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder.inMemoryAuthentication()
                .withUser("singiattend-admin")
                .password(passwordEncoder().encode(serverPassword))
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

