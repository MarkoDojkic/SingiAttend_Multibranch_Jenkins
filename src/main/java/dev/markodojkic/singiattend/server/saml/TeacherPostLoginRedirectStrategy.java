package dev.markodojkic.singiattend.server.saml;

import dev.markodojkic.gateway.strategy.PostLoginRedirectStrategy;
import dev.markodojkic.singiattend.server.repository.IStaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherPostLoginRedirectStrategy implements PostLoginRedirectStrategy {

    @Value("${server.ssl.key-password}")
    private String serverPassword;

    @Value("${app.self-url}")
    private String selfUrl;

    private final IStaffRepository staffRepository;
    private final RestClient restClient;

    @Override
    public String resolve(String postLoginRedirect, Saml2AssertionAuthentication authentication) {
        String id = Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_id");

        HashMap<String, String> payloadMap = new HashMap<>();
        payloadMap.put("id", id);
        payloadMap.put("nameSurname", String.format("%s %s", Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("name"), Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("surname")));
        payloadMap.put("role", staffRepository.getRoleById(id).orElse("INVALID"));
        payloadMap.put("proxyIdentifier", Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_proxy_identifier"));
        if(payloadMap.get("role") != null && !"INVALID".equals(payloadMap.get("role"))) {
            payloadMap.putAll(retrieveCsrfSession(id, payloadMap.get("proxyIdentifier"), authentication.getSaml2Response()));
        }

        String payload = Base64.getUrlEncoder().encodeToString(new JSONObject(payloadMap).toString().getBytes(StandardCharsets.UTF_8));

        return postLoginRedirect + "saml/callback?userContext=" + payload;
    }

    private Map<String, String> retrieveCsrfSession(String id, String proxyIdentifier, String saml2Response) {
        HttpServletRequest originalRequest = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        originalRequest.setAttribute("X-Tenant-Id", proxyIdentifier);
        originalRequest.setAttribute("Authorization", "Basic " + new String(Base64.getEncoder().encode(("singiattend-user:" + serverPassword).getBytes())));

        try {
            var csrfResponse = restClient.post()
                    .uri(UriComponentsBuilder
                            .fromUriString(selfUrl)
                            .path("/api/v1/csrfLogin")
                            .queryParam("loginFor", id)
                            .build()
                            .toUriString())
                    .body(saml2Response)
                    .contentType(MediaType.APPLICATION_XML)
                    .header("X-Tenant-Id", proxyIdentifier)
                    .header("Authorization", "Basic " + new String(Base64.getEncoder().encode(("singiattend-user:" + serverPassword).getBytes())))
                    .retrieve()
                    .toEntity(DefaultCsrfToken.class);

            if (!csrfResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("CSRF token not received from backend for student " + id);
            } else if(csrfResponse.getBody() == null) {
                throw new IllegalStateException("Unexpected response body type from backend CSRF login for student " + id);
            }

            log.debug("CSRF token obtained for staff {}: tokenName={}, token={}", id, csrfResponse.getBody().getHeaderName(), csrfResponse.getBody().getToken());

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
                    "csrfToken", csrfResponse.getBody().getToken());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate student " + id + ": " + e.getMessage(), e);
        }
    }
}
