package dev.markodojkic.singiattend.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class MyBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    @Value("${REALM_NAME}")
    private String realmName;

    @SuppressWarnings("NullableProblems")
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
            throws IOException {
        response.addHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", getRealmName()));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter writer = response.getWriter();
        writer.printf("HTTP Status 401 - %s%n", authEx.getMessage());
    }

    @Override
    public void afterPropertiesSet() {
        setRealmName(realmName);
        super.afterPropertiesSet();
    }
}
