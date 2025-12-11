@echo off
setlocal enabledelayedexpansion

echo Testing Maven deployment configuration...

REM Clean up any previous test
if exist "C:\temp\test-deploy" rmdir /s /q "C:\temp\test-deploy"

echo.
echo === Test 1: Building without DEPLOY_DIR ===
set DEPLOY_DIR=
echo Running: mvnw.cmd clean package -DskipTests
call mvnw.cmd clean package -DskipTests

echo.
echo === Test 2: Building with DEPLOY_DIR ===
set DEPLOY_DIR=C:\temp\test-deploy
echo DEPLOY_DIR is set to: %DEPLOY_DIR%
echo Running: mvnw.cmd package -DskipTests
call mvnw.cmd package -DskipTests

echo.
echo === Results ===
echo Checking if JAR was copied to %DEPLOY_DIR%:
if exist "%DEPLOY_DIR%" (
    echo Directory exists:
    dir "%DEPLOY_DIR%"
) else (
    echo Directory %DEPLOY_DIR% was not created
)

echo.
echo Checking target directory for JAR:
if exist "target\*.jar" (
    dir "target\*.jar"
) else (
    echo No JAR found in target/
)

echo.
echo Test completed!
pause