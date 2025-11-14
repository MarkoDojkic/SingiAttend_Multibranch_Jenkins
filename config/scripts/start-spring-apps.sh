#!/bin/bash
set -e

# 1. Wait for Eureka to be available
echo "⏳ Waiting for Eureka on port 8761..."
until curl -sf http://localhost:8761/eureka/apps > /dev/null; do
  sleep 3
done
echo "✅ Eureka is up."

# 2. Start SingiAttend Server in background
echo "🚀 Starting SingiAttend Server..."
exec java -jar /var/www/SingiAttend-Server.jar &
SERVER_PID=$!

# 3. Wait for SingiAttend Server health check
echo "⏳ Waiting for SingiAttend Server on port 62811..."
until curl -sf http://localhost:8761/eureka/apps/singiattend-server > /dev/null; do
  sleep 3
done
echo "✅ SingiAttend Server is up."

# 4. Start SingiAttend Student Proxy in background
echo "🚀 Starting SingiAttend Student Proxy..."
exec java -jar /var/www/SingiAttend-Student_Proxy.jar &
STUDENT_PROXY_PID=$!

# 5. Wait for SingiAttend Student Proxy health check
echo "⏳ Waiting for SingiAttend Student Proxy on port 62814..."
until curl -sf http://localhost:8761/eureka/apps/singiattend-student-proxy > /dev/null; do
  sleep 3
done
echo "✅ SingiAttend Student Proxy is up."