package dev.markodojkic.singiattend.student_proxy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor forwardHeaders() {
        return requestTemplate -> {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                var csrf = req.getHeader("X-CSRF-TOKEN-SECRET");
                var cookies = req.getHeader("Cookie");
                var xTenantId = req.getHeader("X-Tenant-Id");
                var authorization = req.getHeader("Authorization");
                if (csrf != null) requestTemplate.header("X-CSRF-TOKEN-SECRET", csrf);
                if (cookies != null) requestTemplate.header("Cookie", cookies);
                if (xTenantId != null) requestTemplate.header("X-Tenant-Id", xTenantId);
                if (authorization != null) requestTemplate.header("Authorization", authorization);
            }
        };
    }
}