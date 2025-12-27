package io.sandboxdev.gitmcp.registry;

import io.sandboxdev.gitmcp.model.ResourceContent;
import io.sandboxdev.gitmcp.model.ResourceDefinition;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
        throw new UnsupportedOperationException("Not implemented");
    }
}
