#!/bin/sh
set -e

# ECS injects JWT_PRIVATE_KEY_PEM from Secrets Manager as an env var.
# Spring loads app.jwt.private-key as a Resource (file:/ or classpath:),
# so write the PEM to disk and point JWT_PRIVATE_KEY at that file.
if [ -n "$JWT_PRIVATE_KEY_PEM" ]; then
    mkdir -p /app/keys
    printf '%s\n' "$JWT_PRIVATE_KEY_PEM" > /app/keys/private.pem
    chmod 600 /app/keys/private.pem
    export JWT_PRIVATE_KEY=file:/app/keys/private.pem
fi

if [ -n "$ECS_CONTAINER_METADATA_URI_V4" ]; then
    TASK_IP=$(curl -s "$ECS_CONTAINER_METADATA_URI_V4" | jq -r '.Networks[0].IPv4Addresses[0]')
    if [ -n "$TASK_IP" ] && [ "$TASK_IP" != "null" ]; then
        export EUREKA_INSTANCE_IP_ADDRESS="$TASK_IP"
        export EUREKA_INSTANCE_INSTANCE_ID="$TASK_IP:$HOSTNAME"
    fi
fi

exec java -jar /app/app.jar
