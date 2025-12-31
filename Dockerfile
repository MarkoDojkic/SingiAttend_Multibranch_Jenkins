FROM php:8.2-cli-alpine

RUN apk add --no-cache bash git curl

# Copy app and vendor
COPY . /usr/src/app
WORKDIR /usr/src/app

EXPOSE 9113

CMD ["php", "metrics-exporter.php"]
