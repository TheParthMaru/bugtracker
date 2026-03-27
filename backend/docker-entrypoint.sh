#!/bin/sh
set -e
# Render dashboard env vars override Dockerfile ENV — an empty SPRING_PROFILES_ACTIVE
# would drop the "render" profile. -Dspring.profiles.active wins and keeps console-only logs.
PORT="${PORT:-8080}"
# Visible immediately in Render logs (Spring/Flyway run before Tomcat binds PORT).
echo "bugtracker: starting (PORT=$PORT)"
# Avoid blocking on /dev/random in thin containers (can stall startup 10+ minutes before PORT listens).
exec java \
  -Dspring.profiles.active=render \
  -Dserver.port="$PORT" \
  -Djava.net.preferIPv4Stack=true \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar
