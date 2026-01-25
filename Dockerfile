FROM alpine:edge

# Set up repositories and install runtime dependencies
RUN echo "@edge https://nl.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.16/main" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.16/community" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/main" >> /etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/community" >> /etc/apk/repositories && \
    apk update && apk upgrade && \
    apk add --no-cache \
        bash \
        ca-certificates \
        coreutils \
        curl \
        nginx \
        nss \
        openjdk25 \
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
    update-ca-certificates \
    mkdir -p /var/www/html /etc/supervisor/conf.d /usr/local/share/ca-certificates /var/www

# Copy configs, SSL certs, and scripts
COPY config/nginx.conf /etc/nginx/nginx.conf
COPY config/conf.d /etc/nginx/conf.d/
COPY config/fpm-pool.conf /etc/php81/php-fpm.d/www.conf
COPY config/php.ini /etc/php81/conf.d/custom.ini
COPY config/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY ssl/* /etc/nginx/ssl/

# Copy FE files
COPY --chown=nginx:www-data www /var/www/html/

# Expose FE port
EXPOSE 80

# Start supervisord
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
