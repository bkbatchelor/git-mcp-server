package io.sandboxdev.gitmcp.security;

import org.springframework.stereotype.Component;

@Component
public class GitInputValidator {

    public void validateRepositoryPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Repository path cannot be empty");
        }
        if (containsPathTraversal(path)) {
            throw new IllegalArgumentException("Path traversal detected in repository path");
        }
        // Additional checks can be added here (e.g. allowlist)
    }

    public void validateFilePath(String path) {
        if (path != null && containsPathTraversal(path)) {
            throw new IllegalArgumentException("Path traversal detected in file path");
        }
    }

    public void validateBranchName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }

        // Git branch name rules (simplified)
        if (name.contains("..") ||
                name.contains(" ") ||
                name.contains("~") ||
                name.contains("^") ||
                name.contains(":") ||
                name.contains("?") ||
                name.contains("*") ||
                name.contains("[") ||
                name.contains("@{") ||
                name.contains("\\") ||
                name.contains(";") ||
                name.contains("&") ||
                name.contains("|") ||
                name.contains(">") ||
                name.contains("<") ||
                name.contains("`") ||
                name.contains("$") ||
                name.contains("(") ||
                name.contains(")") ||
                name.contains("'") ||
                name.contains("\"") ||
                name.contains("\n") ||
                name.contains("\r")) {
            throw new IllegalArgumentException("Invalid branch name: " + name);
        }

        if (name.startsWith("-")) {
            throw new IllegalArgumentException("Invalid branch name: " + name);
        }
    }

    public String sanitizeCommitMessage(String message) {
        if (message == null)
            return "";
        return message.trim();
    }

    private boolean containsPathTraversal(String path) {
        return path.contains("..") || path.contains("./") || path.contains(".\\");
    }
}
