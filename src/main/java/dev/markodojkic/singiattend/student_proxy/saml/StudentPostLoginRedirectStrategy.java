package dev.markodojkic.singiattend.student_proxy.saml;

import dev.markodojkic.gateway.strategy.PostLoginRedirectStrategy;
import dev.markodojkic.singiattend.student_proxy.controller.StudentProxyController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentPostLoginRedirectStrategy implements PostLoginRedirectStrategy {

    private final StudentProxyController studentProxyController;

    @Value("${feign_client.backend.username}")
    private String backendUsername;

    @Value("${feign_client.backend.password}")
    private String backendPassword;

    @Override
    public String resolve(String postLoginRedirect, Saml2AssertionAuthentication authentication) {
        String id = Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_id");

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is null or empty in SAML authentication");
        }

        HashMap<String, String> payloadMap = new HashMap<>();
        payloadMap.put("id", id);
        payloadMap.put("proxyIdentifier", Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_proxy_identifier"));
        payloadMap.putAll(loginStudent(id, payloadMap.get("proxyIdentifier"), authentication.getSaml2Response()));

        String payload = Base64.getUrlEncoder().encodeToString(new JSONObject(payloadMap).toString().getBytes(StandardCharsets.UTF_8));

        return postLoginRedirect + "saml/callback?userContext=" + payload;
    }

    private Map<String, String> loginStudent(String id, String proxyIdentifier, String saml2Response) {
        HttpServletRequest originalRequest = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        originalRequest.setAttribute("X-Tenant-Id", proxyIdentifier);
        originalRequest.setAttribute("Authorization", "Basic " + new String(Base64.getEncoder().encode((backendUsername + ":" + backendPassword).getBytes())));

        try {
            var csrfResponse = studentProxyController.csrfLogin(id, saml2Response, originalRequest, "StudentPostLoginRedirectStrategy");

            if (!csrfResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("CSRF token not received from backend for student " + id);
            } else if(csrfResponse.getBody() == null) {
                throw new IllegalStateException("Unexpected response body type from backend CSRF login for student " + id);
            }

            log.debug("CSRF token obtained for student {}: tokenName={}, token={}", id, csrfResponse.getBody().getHeaderName(), csrfResponse.getBody().getToken());

            originalRequest.setAttribute(csrfResponse.getBody().getHeaderName(), csrfResponse.getBody().getToken());

            List<String> setCookies = csrfResponse.getHeaders().get("Set-Cookie");
            if (setCookies != null && !setCookies.isEmpty()) {
                String combined = setCookies.stream()
                        .map(c -> c.split(";", 2)[0].trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("; "));
                if (!combined.isEmpty()) {
                    originalRequest.setAttribute("Cookie", combined);
                }
            }

            String studentName = studentProxyController.getStudentName(id.replace("/", ""), originalRequest.getHeader("X-Client-Device"), originalRequest);
            if (studentName == null || studentName.trim().isEmpty()) {
                throw new IllegalStateException("Student name is empty or null for ID: " + id);
            }
            log.debug("Student {} validated successfully. Name: {}", id, studentName);
            return Map.of("jSessionID", Objects.requireNonNull(csrfResponse.getHeaders().get("Set-Cookie")).stream()
                    .filter(cookie -> cookie.startsWith("JSESSIONID="))
                    .map(cookie -> cookie.substring("JSESSIONID=".length()).split(";")[0])
                    .findFirst()
                    .orElse(""), csrfResponse.getBody().getParameterName(), Objects.requireNonNull(csrfResponse.getHeaders().get("Set-Cookie")).stream()
                    .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                    .map(cookie -> cookie.substring("XSRF-TOKEN=".length()).split(";")[0])
                    .findFirst()
                    .orElse(""),
                    "csrfParameterName", csrfResponse.getBody().getParameterName(),
                    "csrfHeaderName", csrfResponse.getBody().getHeaderName(),
                    "csrfToken", csrfResponse.getBody().getToken(),
                    "studentName", studentName, "status", studentName.isEmpty() ? "INVALID" : "VALID");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate student " + id + ": " + e.getMessage(), e);
        }
    }
}