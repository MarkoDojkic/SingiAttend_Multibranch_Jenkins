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
                var csrf = req.getAttribute("X-CSRF-TOKEN-SECRET").toString();
                var cookies = req.getAttribute("Cookie").toString();
                if (csrf != null) requestTemplate.header("X-CSRF-TOKEN-SECRET", csrf);
                if (cookies != null) requestTemplate.header("Cookie", cookies);
            }
        };
    }
}