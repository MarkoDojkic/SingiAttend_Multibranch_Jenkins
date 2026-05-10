package dev.markodojkic.singiattend.student_proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;


@Configuration
public class FeignConfig {

    @Value("${server.ssl.trust-store}")
    private Resource trustStoreResource;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public RequestInterceptor forwardHeaders() {
        return requestTemplate -> {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest req = attrs.getRequest();

            forwardHeader(req, requestTemplate, "X-CSRF-TOKEN-SECRET");
            forwardHeader(req, requestTemplate, "Cookie");
            forwardHeader(req, requestTemplate, "X-Tenant-Id");
            forwardHeader(req, requestTemplate, "Authorization");
        };
    }
    // headers that must not be split when forwarding
    private static final Set<String> NO_SPLIT = Set.of("authorization", "cookie", "set-cookie");

    private void forwardHeader(HttpServletRequest req, RequestTemplate requestTemplate, String name) {
        Enumeration<String> values = req.getHeaders(name);

        String nameLower = name.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if (values != null) {
            while (values.hasMoreElements()) {
                String v = values.nextElement();
                if (v == null || v.isEmpty()) continue;
                if (NO_SPLIT.contains(nameLower)) {
                    out.add(v);
                } else {
                    for (String part : v.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) out.add(trimmed);
                    }
                }
            }
        }

        // fallback: if no header values (or attribute provides extra), check request attributes
        Object attr = req.getAttribute(name);
        if (attr != null) {
            String av = String.valueOf(attr).trim();
            if (!av.isEmpty()) {
                if (NO_SPLIT.contains(nameLower)) {
                    if (!out.contains(av)) out.add(av);
                } else {
                    for (String part : av.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty() && !out.contains(trimmed)) out.add(trimmed);
                    }
                }
            }
        }

        if (!out.isEmpty()) requestTemplate.header(name, out);
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

}