#!/bin/bash

echo "Testing Maven deployment configuration..."

# Clean up any previous test
rm -rf /tmp/test-deploy

echo ""
echo "=== Test 1: Building without DEPLOY_DIR ==="
unset DEPLOY_DIR
echo "Running: ./mvnw clean package -DskipTests"
./mvnw clean package -DskipTests

echo ""
echo "=== Test 2: Building with DEPLOY_DIR ==="
export DEPLOY_DIR="/tmp/test-deploy"
echo "DEPLOY_DIR is set to: $DEPLOY_DIR"
echo "Running: ./mvnw package -DskipTests"
./mvnw package -DskipTests

echo ""
echo "=== Results ==="
echo "Checking if JAR was copied to $DEPLOY_DIR:"
if [ -d "$DEPLOY_DIR" ]; then
    echo "Directory exists:"
    ls -la "$DEPLOY_DIR/"
else
    echo "Directory $DEPLOY_DIR was not created"
fi

echo ""
echo "Checking target directory for JAR:"
ls -la target/*.jar 2>/dev/null || echo "No JAR found in target/"

echo ""
echo "Test completed!"