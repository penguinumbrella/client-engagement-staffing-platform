#!/bin/sh
set -e

if [ -n "$ECS_CONTAINER_METADATA_URI_V4" ]; then
    TASK_IP=$(curl -s "$ECS_CONTAINER_METADATA_URI_V4" | jq -r '.Networks[0].IPv4Addresses[0]')
    if [ -n "$TASK_IP" ] && [ "$TASK_IP" != "null" ]; then
        export EUREKA_INSTANCE_IP_ADDRESS="$TASK_IP"
    fi
fi

exec java -jar /app/app.jar
