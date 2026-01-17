#!/bin/bash
# git-mcp-server wrapper script for optimized startup

# Get the directory where the script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
JAR="$DIR/build/libs/git-mcp-server-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
    echo "Error: JAR file not found at $JAR" >&2
    echo "Please run './gradlew build' first." >&2
    exit 1
fi

# Optimized JVM flags for fast startup
# -XX:TieredCompilation -XX:TieredStopAtLevel=1: Speeds up JIT by staying at level 1
# -Xms32m -Xmx128m: Moderate memory limits
# -Dspring.main.lazy-initialization=false: Better for the first request
# -Dspring.backgroundpreinitializer.ignore=true: Avoid extra background threads competing for CPU during boot
exec java \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Xms32m -Xmx128m \
    -Dspring.main.lazy-initialization=false \
    -Dspring.backgroundpreinitializer.ignore=true \
    -jar "$JAR" "$@"
