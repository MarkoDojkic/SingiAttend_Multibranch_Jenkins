package dev.markodojkic.singiattend.server.config;

import dev.markodojkic.singiattend.server.auth.MyBasicAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Value("${server.ssl.key-password}")
    private String serverPassword;

    @Value("${allowed-origins}")
    private String allowedOrigins;

    @Value("${server.ssl.trust-store}")
    private Resource trustStoreResource;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN-SECRET"));
        configuration.setAllowCredentials(true); // Needed for cookies/auth

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean("mainSecurityFilterChain")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        CsrfTokenRequestHandler tokenHandler = new XorCsrfTokenRequestAttributeHandler();
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN-SECRET");

        http.securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic
                        .authenticationEntryPoint(myAuthenticationEntryPoint())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                        .sessionRegistry(sessionRegistry())
                )
                .csrf(csrf -> {
                    RequestMatcher csrfRequired = request -> {
                        String path = request.getRequestURI();
                        return !path.startsWith(request.getContextPath() + "/api/v1/csrfLogin") && !path.startsWith(request.getContextPath() + "/actuator") && !path.startsWith(request.getContextPath() + "/api/v1/invalidateSessionForUser");// require CSRF for all other requests (including GET)
                    };

                    csrf.csrfTokenRepository(csrfTokenRepository)
                            .csrfTokenRequestHandler(tokenHandler)
                            .requireCsrfProtectionMatcher(csrfRequired);
                })
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; style-src 'self';")
                        )
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .permissionsPolicyHeader(permissionsPolicyConfig -> permissionsPolicyConfig.policy("geolocation=(), microphone=(), camera=()"))
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/csrfLogout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .clearAuthentication(true)
                        .logoutSuccessHandler((_, response, _) -> response.setStatus(HttpServletResponse.SC_OK))
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("singiattend-user")
                .password(passwordEncoder().encode(serverPassword))
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

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new RegisterSessionAuthenticationStrategy(sessionRegistry)
                )
        );
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public SSLContext sslContext() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream is = trustStoreResource.getInputStream()) {
            trustStore.load(is, trustStorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        SSLContext.setDefault(sslContext);
        return sslContext;
    }

    @Bean
    @Profile("kubernetes")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(SSLContext sslContext) {
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    @Bean
    @Profile("!kubernetes")
    public RestClient.Builder restClientBuilder(SSLContext sslContext) {
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}