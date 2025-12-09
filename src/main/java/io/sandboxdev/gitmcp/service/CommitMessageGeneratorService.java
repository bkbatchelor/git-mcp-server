package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.CommitType;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Service for generating standardized commit messages.
 */
public interface CommitMessageGeneratorService {
    
    /**
     * Generates a standardized commit message based on staged changes.
     */
    String generateCommitMessage(Path repositoryPath, String summary, String description);
    
    /**
     * Extracts project ID from branch name.
     */
    Optional<String> extractProjectId(String branchName);
    
    /**
     * Determines commit type based on staged files and diff.
     */
    CommitType determineCommitType(Path repositoryPath);
}
