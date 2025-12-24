package io.sandboxdev.gitmcp.model;

/**
 * Git parameter validation utility.
 */
public class GitParameterValidator {

    public static boolean isValidRepositoryPath(String path) {
        if (path == null || path.contains("..")) {
            return false;
        }
        return true;
    }

    public static boolean isValidBranchName(String branchName) {
        if (branchName == null || branchName.contains(" ") || 
            branchName.contains("\n") || branchName.contains("..") ||
            branchName.startsWith(".")) {
            return false;
        }
        return true;
    }

    public static boolean isValidCommitMessage(String message) {
        if (message == null || message.trim().isEmpty() || 
            message.contains("\u0000")) {
            return false;
        }
        return true;
    }

    public static boolean isValidGitReference(String ref) {
        if (ref == null || ref.contains(" ") || 
            ref.contains("..") || ref.startsWith(".")) {
            return false;
        }
        return true;
    }
}
