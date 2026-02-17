package dev.markodojkic.singiattend.student_proxy.config;

import feign.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

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
            if (attrs != null) {
                var req = attrs.getRequest();
                var csrf = req.getAttribute("X-CSRF-TOKEN-SECRET").toString();
                var cookies = req.getAttribute("Cookie").toString();
                var xTenantId = req.getAttribute("X-Tenant-Id").toString();
                var authorization = req.getAttribute("Authorization").toString();
                if (csrf != null) requestTemplate.header("X-CSRF-TOKEN-SECRET", csrf);
                if (cookies != null) requestTemplate.header("Cookie", cookies);
                if (xTenantId != null) requestTemplate.header("X-Tenant-Id", xTenantId);
                if (authorization != null) requestTemplate.header("Authorization", authorization);
            }
        };
    }

    @Bean
    public Client feignClient(HostnameVerifier hostnameVerifier,
                              LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory,
                              List<LoadBalancerFeignRequestTransformer> transformers) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream is = trustStoreResource.getInputStream()) {
            trustStore.load(is, trustStorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        return new FeignBlockingLoadBalancerClient(
                new Client.Default(sslContext.getSocketFactory(), hostnameVerifier),
                loadBalancerClient,
                loadBalancerClientFactory,
                transformers
        );
    }
}