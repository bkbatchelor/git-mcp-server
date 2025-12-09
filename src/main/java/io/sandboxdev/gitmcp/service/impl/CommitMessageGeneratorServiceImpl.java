package io.sandboxdev.gitmcp.service.impl;

import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.CommitType;
import io.sandboxdev.gitmcp.service.CommitMessageGeneratorService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommitMessageGeneratorServiceImpl implements CommitMessageGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(CommitMessageGeneratorServiceImpl.class);
    
    // Pattern to extract project ID from branch names like "feature/PROJ-123" or "bugfix/myproj-456"
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^[^/]+/([A-Za-z]+-\\d+)");
    
    private final JGitRepositoryManager repositoryManager;
    
    public CommitMessageGeneratorServiceImpl(JGitRepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    
    @Override
    public String generateCommitMessage(Path repositoryPath, String summary, String description) {
        logger.debug("Generating commit message for {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            String branchName = repository.getBranch();
            
            // Extract project ID from branch name
            Optional<String> projectId = extractProjectId(branchName);
            
            // Determine commit type
            CommitType commitType = determineCommitType(repositoryPath);
            
            // Build commit message
            StringBuilder message = new StringBuilder();
            
            // Title line: [PROJECT-ID]:TYPE Summary
            if (projectId.isPresent()) {
                message.append("[").append(projectId.get()).append("]:");
            }
            message.append(commitType.name()).append(" ").append(summary);
            
            // Add body if description provided
            if (description != null && !description.isBlank()) {
                message.append("\n\n");
                message.append(description);
            }
            
            String result = message.toString();
            logger.debug("Generated commit message: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to generate commit message", e);
            // Fallback to simple message
            return commitType(repositoryPath).name() + " " + summary;
        }
    }
    
    private CommitType commitType(Path repositoryPath) {
        try {
            return determineCommitType(repositoryPath);
        } catch (Exception e) {
            return CommitType.CHORE;
        }
    }

    
    @Override
    public Optional<String> extractProjectId(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return Optional.empty();
        }
        
        Matcher matcher = PROJECT_ID_PATTERN.matcher(branchName);
        if (matcher.find()) {
            String projectId = matcher.group(1).toUpperCase();
            logger.debug("Extracted project ID: {} from branch: {}", projectId, branchName);
            return Optional.of(projectId);
        }
        
        return Optional.empty();
    }
    
    @Override
    public CommitType determineCommitType(Path repositoryPath) {
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                Status status = git.status().call();
                
                Set<String> allChanges = status.getAdded();
                allChanges.addAll(status.getChanged());
                allChanges.addAll(status.getModified());
                
                // Analyze file patterns to determine commit type
                boolean hasTests = allChanges.stream()
                    .anyMatch(f -> f.contains("test") || f.contains("Test") || f.contains("spec"));
                
                boolean hasDocs = allChanges.stream()
                    .anyMatch(f -> f.endsWith(".md") || f.contains("doc") || f.contains("README"));
                
                boolean hasSourceCode = allChanges.stream()
                    .anyMatch(f -> f.endsWith(".java") || f.endsWith(".ts") || 
                                   f.endsWith(".js") || f.endsWith(".py"));
                
                boolean hasConfig = allChanges.stream()
                    .anyMatch(f -> f.contains("config") || f.endsWith(".yml") || 
                                   f.endsWith(".yaml") || f.endsWith(".json") || 
                                   f.endsWith(".xml") || f.endsWith(".properties"));
                
                // Determine type based on patterns
                if (hasTests && !hasSourceCode) {
                    return CommitType.TEST;
                }
                
                if (hasDocs && !hasSourceCode) {
                    return CommitType.DOCS;
                }
                
                if (hasConfig && !hasSourceCode) {
                    return CommitType.CHORE;
                }
                
                // Default to FEAT for new features or FIX for modifications
                boolean hasNewFiles = !status.getAdded().isEmpty();
                if (hasNewFiles) {
                    return CommitType.FEAT;
                }
                
                return CommitType.FIX;
                
            }
        } catch (Exception e) {
            logger.warn("Failed to determine commit type, defaulting to CHORE", e);
            return CommitType.CHORE;
        }
    }
}
