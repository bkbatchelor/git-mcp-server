# Manual Testing Instructions

## Test the Maven deployment configuration

### Step 1: Test without DEPLOY_DIR
```bash
# Make sure DEPLOY_DIR is not set
unset DEPLOY_DIR

# Build the project
./mvnw clean package -DskipTests

# Check if build succeeded
ls -la target/*.jar
```

### Step 2: Test with DEPLOY_DIR
```bash
# Set the deployment directory
export DEPLOY_DIR="/tmp/test-deploy"

# Build the project
./mvnw package -DskipTests

# Check if the JAR was copied
ls -la "$DEPLOY_DIR/"
```

### Step 3: Verify the profile activation
```bash
# Check if the deploy profile is active
export DEPLOY_DIR="/tmp/test-deploy"
./mvnw help:active-profiles

# Run with verbose output to see what's happening
./mvnw package -DskipTests -X | grep -i "deploy\|copy\|resource"
```

## Expected Results

- **Without DEPLOY_DIR**: Build succeeds, JAR created in `target/`, no copying occurs
- **With DEPLOY_DIR**: Build succeeds, JAR created in `target/`, JAR also copied to `$DEPLOY_DIR/`

## Troubleshooting

If the JAR is not being copied:

1. Check if the profile is being activated:
   ```bash
   export DEPLOY_DIR="/tmp/test"
   ./mvnw help:active-profiles
   ```

2. Run with debug output:
   ```bash
   ./mvnw package -DskipTests -X
   ```

3. Check Maven version compatibility:
   ```bash
   ./mvnw --version
   ```