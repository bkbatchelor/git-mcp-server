package io.sandboxdev.gitmcp.security;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Semaphore;

@Component
public class SecurityGuardrails {

    private final List<String> allowedRepositories;
    private final Semaphore operationSemaphore;

    public SecurityGuardrails(GitMcpProperties properties) {
        this.allowedRepositories = properties.security().allowedRepositories();
        this.operationSemaphore = new Semaphore(properties.security().maxConcurrentOperations());
        
        if (allowedRepositories.isEmpty()) {
            throw new IllegalArgumentException("Repository allowlist cannot be empty");
        }
    }

    public boolean isRepositoryAllowed(String repositoryPath) {
        return allowedRepositories.stream()
                .anyMatch(allowed -> repositoryPath.startsWith(allowed));
    }

    public boolean acquireOperationPermit() {
        return operationSemaphore.tryAcquire();
    }

    public void releaseOperationPermit() {
        operationSemaphore.release();
    }

    public String sanitizeOutput(String output) {
        if (output == null) {
            return "";
        }
        
        return output
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("javascript:", "")
                .replaceAll("data:text/html", "data:text/plain")
                .replaceAll("onerror\\s*=", "")
                .replaceAll("onload\\s*=", "");
    }
}
