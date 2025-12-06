#!/bin/sh

dockerd-entrypoint.sh &

echo "Waiting for Docker daemon to start..."
while ! docker info >/dev/null 2>&1; do
  sleep 1
done

echo "Pulling Java image..."
docker pull eclipse-temurin:21-jdk-alpine

exec java -jar /app/app.jar
