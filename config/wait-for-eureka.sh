#!/bin/bash

# Wait for Eureka to be available
echo "Waiting for Eureka on port 8761..."
until curl -s http://localhost:8761/eureka/apps > /dev/null; do
  sleep 3
done

echo "Eureka is up. Starting SingiAttend BE..."
exec java -jar /var/www/SingiAttend-Server-2.5.0-FINAL.jar