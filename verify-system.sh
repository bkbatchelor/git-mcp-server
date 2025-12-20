#!/bin/bash

echo "=== MCP Debugging System Verification ==="
echo

echo "1. Checking Java version..."
java -version
echo

echo "2. Checking Maven version..."
mvn -version
echo

echo "3. Compiling project..."
mvn compile -q
COMPILE_STATUS=$?

if [ $COMPILE_STATUS -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed with status $COMPILE_STATUS"
    exit 1
fi

echo

echo "4. Running tests..."
mvn test -q
TEST_STATUS=$?

if [ $TEST_STATUS -eq 0 ]; then
    echo "✓ All tests passed"
else
    echo "✗ Tests failed with status $TEST_STATUS"
    exit 1
fi

echo
echo "=== System verification complete ==="