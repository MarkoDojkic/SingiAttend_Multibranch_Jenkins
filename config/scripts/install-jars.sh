#!/bin/bash
set -e

# Import SSL certificates into Java cacerts
keytool -import -noprompt -trustcacerts -alias rootCA \
    -file /etc/nginx/ssl/rootCA.pem -storepass changeit -cacerts
keytool -import -noprompt -trustcacerts -alias intermediateCA \
    -file /etc/nginx/ssl/intermediateCA.pem -storepass changeit -cacerts

# Create /var/www if it doesn't exist
mkdir -p /var/www

# Install or keep existing JAR files
for J in SingiAttend-Server.jar eurekaserver.jar SingiAttend-Student_Proxy.jar; do
    if [ -f "/downloads/$J" ]; then
        echo "Installing updated $J"
        cp "/downloads/$J" /var/www/
    elif [ -f "/var/www/$J" ]; then
        echo "Keeping existing $J (no update provided)"
    else
        echo "Warning: $J not found and no update provided"
    fi
done

# Remove temporary downloads
rm -rf /downloads

# Generate fullchain SSL for FE -> BE
cat /etc/nginx/ssl/intermediateCA.pem /etc/nginx/ssl/rootCA.pem \
    > /var/www/html/static/miscellaneous/fullchain_ssl.pem