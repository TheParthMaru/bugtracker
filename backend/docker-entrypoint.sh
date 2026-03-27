#!/bin/sh
set -e
# Render dashboard env vars override Dockerfile ENV — an empty SPRING_PROFILES_ACTIVE
# would drop the "render" profile. -Dspring.profiles.active wins and keeps console-only logs.
exec java \
  -Dspring.profiles.active=render \
  -Dserver.port="${PORT:-8080}" \
  -jar /app/app.jar
