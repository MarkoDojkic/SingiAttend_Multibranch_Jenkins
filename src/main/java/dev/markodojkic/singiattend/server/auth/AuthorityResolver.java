package dev.markodojkic.singiattend.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class AuthorityResolver {

    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");
    private static final int SAN_DNS_NAME_TYPE = 2; // Type 2 is DNSName in X509 SAN

    private AuthorityResolver() {
        /* This utility class should not be instantiated */
    }

    public static List<GrantedAuthority> resolve(String loginFor, HttpServletRequest request, String allowedOrigins, boolean isValidSamlAuthentication) {

        if ("admin".equals(loginFor)) {
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");

            if (certs != null && certs.length > 0 && allowedOrigins != null && !allowedOrigins.isBlank()) {
                X509Certificate cert = certs[0];

                List<String> allowedDomains = Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .map(origin -> origin.replaceAll("https?://", "").split(":")[0])
                        .map(String::toLowerCase)
                        .toList();

                boolean isTrustedPhpFe = false;

                String dn = cert.getSubjectX500Principal().getName();
                Matcher matcher = CN_PATTERN.matcher(dn);
                if (matcher.find()) {
                    String certCn = matcher.group(1).toLowerCase();
                    if (allowedDomains.contains(certCn)) {
                        isTrustedPhpFe = true;
                    }
                }

                if (!isTrustedPhpFe) {
                    try {
                        Collection<List<?>> sanEntries = cert.getSubjectAlternativeNames();
                        if (sanEntries != null) {
                            for (List<?> entry : sanEntries) {
                                // entry.get(0) represents the SAN type. 2 is DNS Name.
                                if (entry.size() >= 2 && entry.get(0) instanceof Integer type && type == SAN_DNS_NAME_TYPE) {
                                    String sanDomain = ((String) entry.get(1)).toLowerCase();
                                    if (allowedDomains.contains(sanDomain)) {
                                        isTrustedPhpFe = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error while checking Subject Alternative Names (SAN) in the certificate", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error while checking SAN");
                    }
                }
                
                if (isTrustedPhpFe) {
                    return List.of(new SimpleGrantedAuthority("ADMIN"));
                }
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is restricted to the trusted frontend only.");
        }

        if (loginFor.matches("\\d{10}")) {
            return List.of(new SimpleGrantedAuthority(isValidSamlAuthentication ? "STUDENT" : "UNAUTHENTICATED_STUDENT"));
        }

        if (loginFor.matches("^[0-9a-fA-F\\-]{24}$")) {
            return List.of(new SimpleGrantedAuthority(isValidSamlAuthentication ? "STAFF" : "UNAUTHENTICATED_STAFF"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid loginFor");
    }
}