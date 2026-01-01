FROM php:8.2-cli-alpine

# Install required tools
RUN apk add --no-cache bash git curl

# Environment variables
ENV APP_NAME=singiattend_fe
ENV METRICS_PORT=9113
ENV FPM_STATUS_PATH=/fpm-status
ENV FPM_SOCKET=/run/php-fpm81.sock

# Copy app
COPY . /usr/src/app
WORKDIR /usr/src/app

EXPOSE ${METRICS_PORT}

CMD ["php", "metrics-exporter.php"]

