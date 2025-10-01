FROM mongo:8.0

LABEL Maintainer="Marko Dojkic <marko.dojkic@gmail.com>"
LABEL Description="Production-ready SingiAttend MongoDB container"

# Install supervisor (cached layer)
RUN apt-get update && \
    apt-get install -y --no-install-recommends supervisor && \
    rm -rf /var/lib/apt/lists/*

# Persist MongoDB data
VOLUME /data/db

# Create supervisor configuration directory
RUN mkdir -p /etc/supervisor/conf.d/

# Copy supervisor config (cached unless changed)
COPY supervisord.conf /etc/supervisor/conf.d/

# Copy static DB init files separately for better caching
COPY SingiAttend_BG /var/tmp/SingiAttend_BG/
COPY SingiAttend_NIS /var/tmp/SingiAttend_NIS/
COPY SingiAttend_NS /var/tmp/SingiAttend_NS/
COPY restore_databases.sh /var/tmp/restore_databases.sh

# Expose MongoDB port
EXPOSE 27017

# Start supervisord
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]