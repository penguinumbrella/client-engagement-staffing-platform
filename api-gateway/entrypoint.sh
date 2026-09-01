#!/bin/sh
set -e

echo "entrypoint: ECS_CONTAINER_METADATA_URI_V4=$ECS_CONTAINER_METADATA_URI_V4"

if [ -n "$ECS_CONTAINER_METADATA_URI_V4" ]; then
    METADATA=$(curl -s "$ECS_CONTAINER_METADATA_URI_V4")
    echo "entrypoint: metadata response: $METADATA"
    TASK_IP=$(echo "$METADATA" | jq -r '.Networks[0].IPv4Addresses[0]')
    echo "entrypoint: resolved TASK_IP=$TASK_IP"
    if [ -n "$TASK_IP" ] && [ "$TASK_IP" != "null" ]; then
        export EUREKA_INSTANCE_IP_ADDRESS="$TASK_IP"
        export EUREKA_INSTANCE_INSTANCE_ID="$TASK_IP:$HOSTNAME"
    fi
fi

exec java -jar /app/app.jar
