package dev.markodojkic.singiattend.server.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN-SECRET");

        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/csrfLogin").permitAll()
                        .requestMatchers("/api/**").authenticated()  // ✅ Only allow /api/** for authenticated users
                        .anyRequest().denyAll()                      // ⛔ Deny everything else automatically
                )
                .httpBasic(customizer -> customizer.authenticationEntryPoint(myAuthenticationEntryPoint()))
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .cors(Customizer.withDefaults())
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                .headers(headers -> headers
                        .xssProtection(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; object-src 'none'; style-src 'self';"
                        ))
                        .frameOptions().sameOrigin()
                ).logout(logout -> logout
                        .logoutUrl("/api/csrfLogout")
                        .invalidateHttpSession(true)  // 🔥 session.invalidate()
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")  // 🔥 delete these cookies
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK))
                )
                .build();
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

