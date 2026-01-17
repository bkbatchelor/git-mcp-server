package io.sandboxdev.gitmcpserver.git;

import io.sandboxdev.gitmcpserver.mcp.Tool;
import io.sandboxdev.gitmcpserver.mcp.ToolRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.List;

@Component
public class GitToolsInitializer {
    private final ToolRegistry toolRegistry;
    private final GitService gitService;

    public GitToolsInitializer(ToolRegistry toolRegistry, GitService gitService) {
        this.toolRegistry = toolRegistry;
        this.gitService = gitService;
    }

    @PostConstruct
    public void init() {
        toolRegistry.registerTool(new Tool(
            "list_branches",
            "List all branches in the repository",
            Map.of(),
            arguments -> {
                List<String> branches = gitService.listBranches();
                return Map.of("content", List.of(Map.of("type", "text", "text", String.join("\n", branches))));
            }
        ));
    }
}
