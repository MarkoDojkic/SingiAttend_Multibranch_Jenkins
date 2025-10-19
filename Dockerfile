# syntax=docker/dockerfile:1.4
FROM alpine:edge

LABEL Maintainer="Marko Dojkic <marko.dojkic@gmail.com>"
LABEL Description="Docker container for SingiAttend BACK-END server, Eureka server, and teacher FRONT-END PHP web app"

# Build arguments for Nexus and versions
ARG NEXUS_URL
ARG NEXUS_USER
ARG NEXUS_PASS
ARG BE_JAR_VERSION
ARG BE_JAR_SUFIX
ARG EUREKA_JAR_VERSION
ARG EUREKA_JAR_SUFIX
ARG STUDENT_PROXY_JAR_VERSION
ARG STUDENT_PROXY_JAR_SUFIX

# Update repositories, install packages, Java, PHP, Nginx, Supervisor, and cleanup cache
RUN echo "@edge http://nl.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories && \
    echo "http://dl-cdn.alpinelinux.org/alpine/v3.16/main" >> /etc/apk/repositories && \
    echo "http://dl-cdn.alpinelinux.org/alpine/v3.16/community" >> /etc/apk/repositories && \
    apk update && apk upgrade && \
    apk add --no-cache \
        bash \
        ca-certificates \
        coreutils \
        curl \
        nginx \
        nss \
        openjdk17 \
        openrc \
        php81 \
        php81-ctype \
        php81-curl \
        php81-dom \
        php81-fpm \
        php81-gd \
        php81-intl \
        php81-mbstring \
        php81-mysqli \
        php81-opcache \
        php81-openssl \
        php81-phar \
        php81-session \
        php81-simplexml \
        php81-xml \
        php81-xmlreader \
        supervisor \
        tzdata && \
    update-ca-certificates && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /var/www/html /etc/supervisor/conf.d /usr/local/share/ca-certificates

# Copy configuration files
COPY config/nginx.conf /etc/nginx/nginx.conf
COPY config/conf.d /etc/nginx/conf.d/
COPY config/fpm-pool.conf /etc/php81/php-fpm.d/www.conf
COPY config/php.ini /etc/php81/conf.d/custom.ini
COPY config/ssl* /etc/nginx/ssl/
COPY config/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY config/start-spring-apps.sh /etc/supervisor/conf.d/start-spring-apps.sh
RUN chmod +x /etc/supervisor/conf.d/start-spring-apps.sh

# Copy and import certificates
COPY ssl/rootCA.pem /usr/local/share/ca-certificates/rootCA.pem
COPY ssl/intermediateCA.pem /usr/local/share/ca-certificates/intermediateCA.pem

RUN keytool -import -noprompt -trustcacerts -alias rootCA \
        -file /usr/local/share/ca-certificates/rootCA.pem -storepass changeit -cacerts && \
    keytool -import -noprompt -trustcacerts -alias intermediateCA \
        -file /usr/local/share/ca-certificates/intermediateCA.pem -storepass changeit -cacerts

# Copy FE files
COPY --chown=nginx:www-data www /var/www/html/

RUN set -eux; \
    curl -fSL -u "$NEXUS_USER:$NEXUS_PASS" \
        "$NEXUS_URL/repository/maven-snapshots/dev/markodojkic/SingiAttend-Server/$BE_JAR_VERSION/SingiAttend-Server-$BE_JAR_SUFIX.jar" \
        -o "/var/www/SingiAttend-Server.jar"; \
    curl -fSL -u "$NEXUS_USER:$NEXUS_PASS" \
        "$NEXUS_URL/repository/maven-releases/dev/markodojkic/eurekaserver/$EUREKA_JAR_VERSION/eurekaserver-$EUREKA_JAR_SUFIX.jar" \
        -o "/var/www/eurekaserver.jar" \
    curl -fSL -u "$NEXUS_USER:$NEXUS_PASS" \
        "$NEXUS_URL/repository/maven-releases/dev/markodojkic/studentproxy/$STUDENT_PROXY_JAR_VERSION/SingiAttend-Student_Proxy-$STUDENT_PROXY_JAR_SUFIX.jar" \
        -o "/var/www/SingiAttend-Student_Proxy.jar"

# Add fullchain SSL for FE -> BE communication
RUN cat /usr/local/share/ca-certificates/intermediateCA.pem /usr/local/share/ca-certificates/rootCA.pem \
    > /var/www/html/static/miscellaneous/fullchain_ssl.pem

# Expose FE, BE, and Eureka ports
EXPOSE 443 62811 8761

# Start supervisord
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]