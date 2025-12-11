@echo off
setlocal

set LOG_DIR=logs
set LOG_FILE=%LOG_DIR%\git-mcp-server.log

if "%1"=="" goto show_usage
if "%1"=="help" goto show_usage

if "%1"=="tail" goto tail_log
if "%1"=="view" goto view_log
if "%1"=="errors" goto show_errors
if "%1"=="warnings" goto show_warnings
if "%1"=="info" goto show_info
if "%1"=="size" goto show_size
if "%1"=="clean" goto clean_logs
if "%1"=="rotate" goto rotate_info

echo Unknown command: %1
echo.
goto show_usage

:show_usage
echo Git MCP Server Log Management
echo.
echo Usage: %0 [command]
echo.
echo Commands:
echo   tail      - Follow the current log file (requires PowerShell)
echo   view      - View the current log file
echo   errors    - Show only ERROR level logs
echo   warnings  - Show only WARN level logs
echo   info      - Show only INFO level logs
echo   size      - Show log file sizes
echo   clean     - Clean old compressed log files
echo   rotate    - Force log rotation (requires server restart)
echo   help      - Show this help message
goto end

:tail_log
echo Following log file: %LOG_FILE%
echo Press Ctrl+C to stop
if exist "%LOG_FILE%" (
    powershell -Command "Get-Content '%LOG_FILE%' -Wait"
) else (
    echo Log file not found: %LOG_FILE%
)
goto end

:view_log
echo Viewing log file: %LOG_FILE%
if exist "%LOG_FILE%" (
    type "%LOG_FILE%"
) else (
    echo Log file not found: %LOG_FILE%
)
goto end

:show_errors
echo Showing ERROR level logs:
if exist "%LOG_FILE%" (
    findstr "ERROR" "%LOG_FILE%"
) else (
    echo Log file not found: %LOG_FILE%
)
goto end

:show_warnings
echo Showing WARN level logs:
if exist "%LOG_FILE%" (
    findstr "WARN" "%LOG_FILE%"
) else (
    echo Log file not found: %LOG_FILE%
)
goto end

:show_info
echo Showing INFO level logs:
if exist "%LOG_FILE%" (
    findstr "INFO" "%LOG_FILE%"
) else (
    echo Log file not found: %LOG_FILE%
)
goto end

:show_size
echo Log file sizes:
if exist "%LOG_DIR%" (
    dir "%LOG_DIR%"
) else (
    echo Log directory not found: %LOG_DIR%
)
goto end

:clean_logs
echo Cleaning old compressed log files...
if exist "%LOG_DIR%" (
    forfiles /p "%LOG_DIR%" /m "*.log.gz" /d -30 /c "cmd /c del @path"
    echo Cleanup completed
) else (
    echo Log directory not found: %LOG_DIR%
)
goto end

:rotate_info
echo To force log rotation, restart the Git MCP Server
echo The server will automatically rotate logs based on size and time
goto end

:end