package dev.markodojkic.singiattend.student_proxy.config;

import dev.markodojkic.singiattend.student_proxy.client.KubernetesAwareHostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

@Configuration
public class HostnameVerifierConfig {

    @Bean(name = "hostnameVerifier")
    @Profile("dev")
    public HostnameVerifier defaultHostnameVerifier() {
        return HttpsURLConnection.getDefaultHostnameVerifier();
    }


    @Bean(name = "hostnameVerifier")
    @Profile("kubernetes")
    public HostnameVerifier kubernetesHostnameVerifier() {
        return new KubernetesAwareHostnameVerifier();
    }
}