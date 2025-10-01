FROM alpine:edge

LABEL Maintainer="Marko Dojkic <marko.dojkic@gmail.com>"
LABEL Description="Docker container for running 'SingiAttend' BACK-END server and teacher FRONT-END web app written in PHP on Alpine Linux"
RUN apk add -X https://nl.alpinelinux.org/alpine/edge/main -u alpine-keys --allow-untrusted
RUN echo "@edge http://nl.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories
#Adding alpine repositories for php8.1
RUN echo 'http://dl-cdn.alpinelinux.org/alpine/v3.16/main' >> /etc/apk/repositories
RUN echo 'http://dl-cdn.alpinelinux.org/alpine/v3.16/community' >> /etc/apk/repositories

#Install needed packages
RUN apk update && apk upgrade
RUN apk add openrc \
  supervisor \
  nginx \
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
  php81-xmlreader

RUN apk add ca-certificates \
  && update-ca-certificates \
  && apk add --update coreutils && rm -rf /var/cache/apk/*   \ 
  && apk add --update openjdk17 tzdata unzip curl bash \
  && apk add --no-cache nss \
  && rm -rf /var/cache/apk/*  

# Copy ngix, php81-fpm configurations and ssl certificates
COPY config/nginx.conf /etc/nginx/nginx.conf
COPY config/conf.d /etc/nginx/conf.d/
COPY config/fpm-pool.conf /etc/php81/php-fpm.d/www.conf 
COPY config/php.ini /etc/php81/conf.d/custom.ini
COPY config/ssl* /etc/nginx/ssl/

# Copy and import the certificates into the container
COPY ssl/rootCA.pem /usr/local/share/ca-certificates/rootCA.pem
COPY ssl/intermediateCA.pem /usr/local/share/ca-certificates/intermediateCA.pem

RUN keytool -import -noprompt -trustcacerts -alias rootCA -file /usr/local/share/ca-certificates/rootCA.pem -storepass changeit -cacerts && \
    keytool -import -noprompt -trustcacerts -alias intermediateCA -file /usr/local/share/ca-certificates/intermediateCA.pem -storepass changeit -cacerts

# Configure nginx FE, BE and Eureka
RUN mkdir /var/www/html
COPY --chown=nginx www /var/www/html/
ADD eureka-server-0.0.0.jar /var/www/eureka-server-0.0.0.jar
ADD SingiAttend-Server-2.5.0-FINAL.jar /var/www/SingiAttend-Server-2.5.0-FINAL.jar

#Add fullchain SSL for FE -> BE communication
RUN cat /usr/local/share/ca-certificates/intermediateCA.pem /usr/local/share/ca-certificates/rootCA.pem > /var/www/html/static/miscellaneous/fullchain_ssl.pem

#Expose FE, BE and eureka ports
EXPOSE 443 62811 8761

# Configure supervisord
COPY config/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY config/wait-for-eureka.sh /etc/supervisor/conf.d/wait-for-eureka.sh
RUN chmod +x /etc/supervisor/conf.d/wait-for-eureka.sh

# Let supervisord start nginx & php-fpm & proxy app 
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]