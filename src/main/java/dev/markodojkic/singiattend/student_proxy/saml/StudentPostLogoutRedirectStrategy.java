package dev.markodojkic.singiattend.student_proxy.saml;

import dev.markodojkic.gateway.strategy.DefaultSamlLogoutSuccessStrategy;
import dev.markodojkic.singiattend.student_proxy.controller.StudentProxyController;
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
public class StudentPostLogoutRedirectStrategy extends DefaultSamlLogoutSuccessStrategy {

    private final StudentProxyController studentProxyController;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        studentProxyController.csrfLogout(request, "StudentPostLogoutRedirectStrategy");
        super.handle(request, response, authentication);
    }
}