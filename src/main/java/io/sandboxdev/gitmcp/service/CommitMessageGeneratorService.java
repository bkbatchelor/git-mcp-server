package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.CommitType;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Service interface for generating standardized commit messages.
 * 
 * <p>This service provides functionality for creating consistent, well-formatted
 * commit messages that follow conventional commit standards. It analyzes staged
 * changes and branch names to generate appropriate commit messages with project
 * IDs and commit types.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
public interface CommitMessageGeneratorService {
    
    /**
     * Generates a standardized commit message based on staged changes and branch context.
     * 
     * <p>Creates a commit message following the format: "[PROJECT-ID]:TYPE Summary"
     * followed by an optional detailed description. The project ID is extracted from
     * the current branch name, and the commit type is determined by analyzing the
     * staged changes.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param summary a brief summary of the changes (required)
     * @param description optional detailed description of the changes (can be null)
     * @return a formatted commit message following the standardized template
     * @throws GitMcpException if the repository is not found or message generation fails
     * @throws IllegalArgumentException if repositoryPath or summary is null/empty
     */
    String generateCommitMessage(Path repositoryPath, String summary, String description);
    
    /**
     * Extracts a project ID from a Git branch name.
     * 
     * <p>Parses branch names following patterns like "feature/PROJ-123" or
     * "bugfix/PROJ-456" to extract project identifiers. Returns empty if no
     * project ID pattern is found in the branch name.</p>
     * 
     * @param branchName the Git branch name to parse
     * @return an {@link Optional} containing the project ID if found, empty otherwise
     * @throws IllegalArgumentException if branchName is null
     */
    Optional<String> extractProjectId(String branchName);
    
    /**
     * Determines the appropriate commit type based on staged changes.
     * 
     * <p>Analyzes the currently staged files and their changes to determine the
     * most appropriate commit type from: FEAT, FIX, CHORE, REFACTOR, DOCS, STYLE, or TEST.
     * The determination is based on file paths, extensions, and change patterns.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @return the {@link CommitType} that best represents the staged changes
     * @throws GitMcpException if the repository is not found or analysis fails
     * @throws IllegalArgumentException if repositoryPath is null
     */
    CommitType determineCommitType(Path repositoryPath);
}
