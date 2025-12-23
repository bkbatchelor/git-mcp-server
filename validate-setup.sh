#!/bin/bash

# Git MCP Server Setup Validation Script
# This script validates that the project structure and dependencies are correctly configured

echo "🔍 Validating Git MCP Server Setup..."
echo

# Check Java version
echo "📋 Checking Java version..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✅ Java version: $java_version"
    
    # Check if Java 21 or higher
    major_version=$(echo $java_version | cut -d'.' -f1)
    if [ "$major_version" -ge 21 ]; then
        echo "✅ Java 21+ requirement satisfied"
    else
        echo "❌ Java 21 or higher is required"
        exit 1
    fi
else
    echo "❌ Java not found. Please install Java 21 or higher."
    exit 1
fi

echo

# Check Gradle wrapper
echo "📋 Checking Gradle wrapper..."
if [ -f "./gradlew" ]; then
    echo "✅ Gradle wrapper found"
    chmod +x ./gradlew
    
    # Try to run Gradle version
    if ./gradlew --version &> /dev/null; then
        echo "✅ Gradle wrapper is functional"
    else
        echo "⚠️  Gradle wrapper may need initialization"
        echo "   Run: gradle wrapper --gradle-version 8.11.1"
    fi
else
    echo "❌ Gradle wrapper not found"
    exit 1
fi

echo

# Check project structure
echo "📋 Checking project structure..."
required_dirs=(
    "src/main/java/io/sandboxdev/gitmcp"
    "src/main/resources"
    "src/test/java"
    "gradle"
)

for dir in "${required_dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo "✅ Directory exists: $dir"
    else
        echo "❌ Missing directory: $dir"
        exit 1
    fi
done

echo

# Check key files
echo "📋 Checking key configuration files..."
required_files=(
    "build.gradle.kts"
    "gradle/libs.versions.toml"
    "src/main/resources/application.yml"
    "src/main/resources/logback-spring.xml"
    "src/main/java/io/sandboxdev/gitmcp/GitMcpServerApplication.java"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ File exists: $file"
    else
        echo "❌ Missing file: $file"
        exit 1
    fi
done

echo

# Try to compile the project
echo "📋 Testing project compilation..."
if ./gradlew compileJava --no-daemon --quiet; then
    echo "✅ Project compiles successfully"
else
    echo "❌ Compilation failed. Check dependencies and configuration."
    exit 1
fi

echo

# Try to run tests
echo "📋 Testing basic functionality..."
if ./gradlew test --no-daemon --quiet; then
    echo "✅ Basic tests pass"
else
    echo "⚠️  Some tests failed. This may be expected if external dependencies are not configured."
fi

echo
echo "🎉 Git MCP Server setup validation complete!"
echo
echo "Next steps:"
echo "1. Set environment variables: OPENAI_API_KEY, ANTHROPIC_API_KEY"
echo "2. Run the application: ./gradlew bootRun"
echo "3. Run full test suite: ./gradlew test"
echo "4. Run mutation tests: ./gradlew pitest"