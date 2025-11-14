# syntax=docker/dockerfile:1.4

############################################################
# Stage 1: Base image with common runtime dependencies
############################################################
FROM alpine:edge AS base

# Set up repositories and install runtime dependencies
RUN echo "@edge https://nl.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.16/main" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.16/community" >> /etc/apk/repositories && \
    apk update && apk upgrade && \
    apk add --no-cache \
        bash \
        ca-certificates \
        coreutils \
        curl \
        nginx \
        nss \
        openjdk24 \
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
    update-ca-certificates

############################################################
# Stage 2: Builder for downloading JARs from Nexus
############################################################
FROM base AS builder

# Build arguments for versioning and Nexus access
ARG NEXUS_URL
ARG NEXUS_USER
ARG NEXUS_PASS
ARG BE_JAR_VERSION
ARG BE_JAR_SUFFIX
ARG EUREKA_JAR_VERSION
ARG EUREKA_JAR_SUFFIX
ARG STUDENT_PROXY_JAR_VERSION
ARG STUDENT_PROXY_JAR_SUFFIX

SHELL ["/bin/bash", "-c"]

# Install curl
RUN apk add --no-cache curl

# Create downloads directory
RUN mkdir -p /downloads

# Copy a small script that handles JAR downloads
COPY config/scripts/download-jars.sh /tmp/download_jars.sh
RUN chmod +x /tmp/download-jars.sh && /tmp/download-jars.sh

############################################################
# Stage 3: Final image
############################################################
FROM base

LABEL Maintainer="Marko Dojkic <marko.dojkic@gmail.com>"
LABEL Description="Docker container for SingiAttend BACK-END, Eureka server, and PHP FE"

# Create directories
RUN mkdir -p /var/www/html /etc/supervisor/conf.d /usr/local/share/ca-certificates /var/www

# Copy configs, SSL certs, and scripts
COPY config/nginx.conf /etc/nginx/nginx.conf
COPY config/conf.d /etc/nginx/conf.d/
COPY config/fpm-pool.conf /etc/php81/php-fpm.d/www.conf
COPY config/php.ini /etc/php81/conf.d/custom.ini
COPY config/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY config/scripts/start-spring-apps.sh /etc/supervisor/conf.d/start-spring-apps.sh
COPY ssl/* /etc/nginx/ssl/
RUN chmod +x /etc/supervisor/conf.d/start-spring-apps.sh

# Copy FE files
COPY --chown=nginx:www-data www /var/www/html/

# Copy JARs from builder if downloaded
COPY --from=builder /downloads/ /downloads/

# Copy and execute script to import certs and handle JARs
COPY config/scripts/install-jars.sh /tmp/install-jars.sh
RUN chmod +x /tmp/install-jars.sh && /tmp/install-jars.sh

# Expose FE, BE, Student Proxy  and Eureka ports
EXPOSE 443 62811 62814 8761

# Start supervisord
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]