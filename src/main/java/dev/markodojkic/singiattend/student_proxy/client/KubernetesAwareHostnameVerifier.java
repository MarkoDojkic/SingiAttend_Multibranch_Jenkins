package dev.markodojkic.singiattend.student_proxy.client;

import lombok.SneakyThrows;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.net.InetAddress;
import java.util.regex.Pattern;

public class KubernetesAwareHostnameVerifier implements HostnameVerifier {

    private static final Pattern IP_PATTERN =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$");

    private final HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

    @SneakyThrows
    @Override
    public boolean verify(String host, SSLSession session) {
        if (isIpAddress(host)) {
            InetAddress inetAddress = InetAddress.getByName(host);
            String fqdn = inetAddress.getCanonicalHostName().split(host.replace("\\.", "-"))[1];

            return defaultVerifier.verify(fqdn, session);
        } else {
            return defaultVerifier.verify(host, session);
        }
    }

    private boolean isIpAddress(String host) {
        return IP_PATTERN.matcher(host).matches();
    }
}
