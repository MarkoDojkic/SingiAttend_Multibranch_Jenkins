package dev.markodojkic.singiattend.server.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${server.ssl.key-password}")
    private String serverPassword;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://localhost:62812",           // FE dev
                "https://kangaroo-discrete-viper.ngrok-free.app" // Static ngrok URL for mobile
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN-SECRET"));
        configuration.setAllowCredentials(true); // Needed for cookies/auth

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestHandler tokenHandler = new XorCsrfTokenRequestAttributeHandler();
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN-SECRET");

        http.securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/csrfLogin").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic
                .authenticationEntryPoint(myAuthenticationEntryPoint())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(tokenHandler)
                .ignoringRequestMatchers("/api/csrfLogin")
            )
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource())
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; style-src 'self';")
                )
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .permissionsPolicyHeader(policy -> policy
                        .policy("geolocation=(), microphone=(), camera=()"))
            )
            .logout(logout -> logout
                .logoutUrl("/api/csrfLogout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .clearAuthentication(true)
                .logoutSuccessHandler((request, response, authentication) -> 
                    response.setStatus(HttpServletResponse.SC_OK))
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("singiattend-admin")
            .password(passwordEncoder().encode(serverPassword))
            .roles("ADMIN")
            .authorities("singiattend")
            .build();

        return new InMemoryUserDetailsManager(user);
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

