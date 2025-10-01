FROM mongo:8.0

LABEL Maintainer="Marko Dojkic <marko.dojkic@gmail.com>"
LABEL Description="Docker container for running 'SingiAttend' Mongo Database"

RUN apt-get update && \
    apt-get install -y supervisor && \
    apt-get clean

VOLUME /data/db

RUN mkdir -p /etc/supervisor/conf.d/
COPY . /var/tmp/
RUN rm /var/tmp/Dockerfile
RUN mv /var/tmp/supervisord.conf /etc/supervisor/conf.d/

#Expose MongoDB port
EXPOSE 27017

# Let supervisord start nginx & php-fpm & mongodb & mongoimport for database & java 
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]