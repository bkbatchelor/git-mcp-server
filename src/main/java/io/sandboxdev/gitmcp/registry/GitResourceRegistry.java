package io.sandboxdev.gitmcp.registry;

import io.sandboxdev.gitmcp.model.ResourceContent;
import io.sandboxdev.gitmcp.model.ResourceDefinition;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GitResourceRegistry {
    private final JGitRepositoryManager repositoryManager;

    public GitResourceRegistry(JGitRepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    public List<ResourceDefinition> listResources() {
        return Collections.emptyList();
    }

    public ResourceContent readResource(String uri) {
        if (!uri.startsWith("git://")) {
            throw new IllegalArgumentException("Invalid URI scheme: " + uri);
        }

        // URI format: git://{repoPath}/file/{filePath}
        int fileMarker = uri.indexOf("/file/");
        if (fileMarker == -1) {
            throw new IllegalArgumentException("Invalid URI format. Expected /file/ marker: " + uri);
        }

        String repoPathStr = uri.substring(6, fileMarker); // 6 is "git://".length()
        String filePathStr = uri.substring(fileMarker + 6); // 6 is "/file/".length()

        java.nio.file.Path repoPath = java.nio.file.Paths.get(repoPathStr);
        if (!repositoryManager.isValidRepository(repoPath)) {
            throw new IllegalArgumentException("Invalid repository path: " + repoPathStr);
        }

        java.nio.file.Path fullPath = repoPath.resolve(filePathStr);
        try {
            if (!java.nio.file.Files.exists(fullPath)) {
                throw new IllegalArgumentException("File not found: " + fullPath);
            }

            String text = java.nio.file.Files.readString(fullPath);
            // Simple MIME type detection (can be improved)
            String mimeType = "text/plain";

            return new ResourceContent(uri, mimeType, text);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read resource: " + uri, e);
        }
    }
}
