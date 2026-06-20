package dev.markodojkic.singiattend.server.saml;

import dev.markodojkic.gateway.strategy.DefaultSamlLogoutSuccessStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherPostLogoutRedirectStrategy extends DefaultSamlLogoutSuccessStrategy {
    @Value("${app.self-url}")
    private String selfUrl;

    private final RestClient restClient;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        restClient.post()
                .uri(UriComponentsBuilder
                        .fromUriString(selfUrl)
                        .path("/api/v1/csrfLogout")
                        .build()
                        .toUriString());
        super.handle(request, response, authentication);
    }
}
