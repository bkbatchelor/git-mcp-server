package io.sandboxdev.gitmcpserver.git;

import io.sandboxdev.gitmcpserver.mcp.Tool;
import io.sandboxdev.gitmcpserver.mcp.ToolRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Lazy(false)
public class GitToolsInitializer {
    private final ToolRegistry toolRegistry;
    private final GitService gitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitToolsInitializer(ToolRegistry toolRegistry, GitService gitService) {
        this.toolRegistry = toolRegistry;
        this.gitService = gitService;
    }

    @PostConstruct
    public void init() {
        toolRegistry.registerTool(new Tool(
            "list_branches",
            "List all branches in the repository",
            Map.of("type", "object", "properties", Map.of()),
            arguments -> {
                List<String> branches = gitService.listBranches();
                return Map.of("content", List.of(Map.of("type", "text", "text", String.join("\n", branches))));
            }
        ));

        toolRegistry.registerTool(new Tool(
            "checkout_branch",
            "Checkout a branch",
            Map.of("type", "object", "properties", Map.of("branch_name", Map.of("type", "string")), "required", List.of("branch_name")),
            arguments -> {
                String branchName = (String) arguments.get("branch_name");
                gitService.checkoutBranch(branchName);
                return Map.of("content", List.of(Map.of("type", "text", "text", "Checked out branch: " + branchName)));
            }
        ));

        toolRegistry.registerTool(new Tool(
            "get_log",
            "Get commit log",
            Map.of("type", "object", "properties", Map.of("count", Map.of("type", "integer"))),
            arguments -> {
                int count = 10;
                if (arguments.containsKey("count")) {
                    count = ((Number) arguments.get("count")).intValue();
                }
                List<Map<String, String>> log = gitService.getLog(count);
                try {
                    String logJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(log);
                    return Map.of("content", List.of(Map.of("type", "text", "text", logJson)));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize log", e);
                }
            }
        ));
    }
}
