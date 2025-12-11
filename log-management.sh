#!/bin/bash

# Git MCP Server Log Management Script

LOG_DIR="logs"
LOG_FILE="$LOG_DIR/git-mcp-server.log"

show_usage() {
    echo "Git MCP Server Log Management"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  tail      - Follow the current log file"
    echo "  view      - View the current log file"
    echo "  errors    - Show only ERROR level logs"
    echo "  warnings  - Show only WARN level logs"
    echo "  info      - Show only INFO level logs"
    echo "  size      - Show log file sizes"
    echo "  clean     - Clean old compressed log files (older than 30 days)"
    echo "  rotate    - Force log rotation (requires server restart)"
    echo "  help      - Show this help message"
}

case "$1" in
    "tail")
        echo "Following log file: $LOG_FILE"
        echo "Press Ctrl+C to stop"
        tail -f "$LOG_FILE" 2>/dev/null || echo "Log file not found: $LOG_FILE"
        ;;
    "view")
        echo "Viewing log file: $LOG_FILE"
        less "$LOG_FILE" 2>/dev/null || echo "Log file not found: $LOG_FILE"
        ;;
    "errors")
        echo "Showing ERROR level logs:"
        grep "ERROR" "$LOG_FILE" 2>/dev/null || echo "No errors found or log file not found"
        ;;
    "warnings")
        echo "Showing WARN level logs:"
        grep "WARN" "$LOG_FILE" 2>/dev/null || echo "No warnings found or log file not found"
        ;;
    "info")
        echo "Showing INFO level logs:"
        grep "INFO" "$LOG_FILE" 2>/dev/null || echo "No info logs found or log file not found"
        ;;
    "size")
        echo "Log file sizes:"
        if [ -d "$LOG_DIR" ]; then
            ls -lh "$LOG_DIR"/ 2>/dev/null || echo "No log files found"
        else
            echo "Log directory not found: $LOG_DIR"
        fi
        ;;
    "clean")
        echo "Cleaning old compressed log files (older than 30 days)..."
        if [ -d "$LOG_DIR" ]; then
            find "$LOG_DIR" -name "*.log.gz" -mtime +30 -delete -print
            echo "Cleanup completed"
        else
            echo "Log directory not found: $LOG_DIR"
        fi
        ;;
    "rotate")
        echo "To force log rotation, restart the Git MCP Server"
        echo "The server will automatically rotate logs based on size and time"
        ;;
    "help"|"")
        show_usage
        ;;
    *)
        echo "Unknown command: $1"
        echo ""
        show_usage
        exit 1
        ;;
esac