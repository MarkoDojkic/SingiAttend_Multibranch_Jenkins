package dev.markodojkic.singiattend.server.saml;

import dev.markodojkic.gateway.strategy.PostLoginRedirectStrategy;
import dev.markodojkic.singiattend.server.repository.IStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherPostLoginRedirectStrategy implements PostLoginRedirectStrategy {

    private final IStaffRepository staffRepository;

    @Override
    public String resolve(String postLoginRedirect, Saml2AssertionAuthentication authentication) {
        String id = Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_id");

        HashMap<String, String> payloadMap = new HashMap<>();
        payloadMap.put("id", id);
        payloadMap.put("nameSurname", String.format("%s %s", Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("name"), Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("surname")));
        payloadMap.put("role", staffRepository.getRoleById(id).orElse("INVALID"));
        payloadMap.put("proxyIdentifier", Objects.requireNonNull(authentication.getCredentials()).getFirstAttribute("singiattend_proxy_identifier"));

        String payload = Base64.getUrlEncoder().encodeToString(new JSONObject(payloadMap).toString().getBytes(StandardCharsets.UTF_8));

        return postLoginRedirect + "saml/callback?userContext=" + payload;
    }
}
