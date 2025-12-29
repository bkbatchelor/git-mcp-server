package io.sandboxdev.gitmcp.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class GitInputValidator {

    private final SecurityGuardrails securityGuardrails;

    public GitInputValidator(SecurityGuardrails securityGuardrails) {
        this.securityGuardrails = securityGuardrails;
    }

    public void validateRepositoryPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Repository path cannot be empty");
        }
        if (containsPathTraversal(path)) {
            throw new IllegalArgumentException("Path traversal detected in repository path");
        }
        if (!securityGuardrails.isRepositoryAllowed(path)) {
            throw new IllegalArgumentException("Repository path not in allowlist: " + path);
        }
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

    public boolean validateToolParameters(String toolName, JsonNode parameters) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }
        
        if (parameters == null) {
            return false;
        }
        
        // Basic schema validation for known tools
        return switch (toolName) {
            case "git_status" -> validateGitStatusParameters(parameters);
            case "git_commit" -> validateGitCommitParameters(parameters);
            case "git_diff" -> validateGitDiffParameters(parameters);
            case "git_branch_list" -> validateGitBranchListParameters(parameters);
            case "git_branch_create" -> validateGitBranchCreateParameters(parameters);
            case "git_checkout" -> validateGitCheckoutParameters(parameters);
            case "git_log" -> validateGitLogParameters(parameters);
            default -> false; // Unknown tool
        };
    }

    private boolean validateGitStatusParameters(JsonNode parameters) {
        // git_status takes no parameters
        return parameters.isEmpty() || parameters.isObject();
    }

    private boolean validateGitCommitParameters(JsonNode parameters) {
        if (!parameters.isObject()) return false;
        
        JsonNode message = parameters.get("message");
        if (message == null || !message.isTextual() || message.asText().isBlank()) {
            return false;
        }
        
        // Optional allowEmpty parameter
        JsonNode allowEmpty = parameters.get("allowEmpty");
        if (allowEmpty != null && !allowEmpty.isBoolean()) {
            return false;
        }
        
        return true;
    }

    private boolean validateGitDiffParameters(JsonNode parameters) {
        if (!parameters.isObject()) return false;
        
        // All parameters are optional
        JsonNode fromRef = parameters.get("fromRef");
        if (fromRef != null && !fromRef.isTextual()) return false;
        
        JsonNode toRef = parameters.get("toRef");
        if (toRef != null && !toRef.isTextual()) return false;
        
        JsonNode filePath = parameters.get("filePath");
        if (filePath != null && !filePath.isTextual()) return false;
        
        return true;
    }

    private boolean validateGitBranchListParameters(JsonNode parameters) {
        // git_branch_list takes no parameters
        return parameters.isEmpty() || parameters.isObject();
    }

    private boolean validateGitBranchCreateParameters(JsonNode parameters) {
        if (!parameters.isObject()) return false;
        
        JsonNode branchName = parameters.get("branchName");
        return branchName != null && branchName.isTextual() && !branchName.asText().isBlank();
    }

    private boolean validateGitCheckoutParameters(JsonNode parameters) {
        if (!parameters.isObject()) return false;
        
        JsonNode branchName = parameters.get("branchName");
        return branchName != null && branchName.isTextual() && !branchName.asText().isBlank();
    }

    private boolean validateGitLogParameters(JsonNode parameters) {
        if (!parameters.isObject()) return false;
        
        // All parameters are optional
        JsonNode limit = parameters.get("limit");
        if (limit != null && !limit.isInt()) return false;
        
        JsonNode filePath = parameters.get("filePath");
        if (filePath != null && !filePath.isTextual()) return false;
        
        JsonNode since = parameters.get("since");
        if (since != null && !since.isTextual()) return false;
        
        return true;
    }

    private boolean containsPathTraversal(String path) {
        return path.contains("..") || path.contains("./") || path.contains(".\\");
    }
}
