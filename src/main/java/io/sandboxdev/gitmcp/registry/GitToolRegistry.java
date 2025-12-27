package io.sandboxdev.gitmcp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.ToolDefinition;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.*;
import io.sandboxdev.gitmcp.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class GitToolRegistry {
    private final Map<String, ToolHandler> toolHandlers = new HashMap<>();
    private final List<ToolDefinition> toolDefinitions = new ArrayList<>();
    private final ObjectMapper objectMapper;

    public GitToolRegistry(
            GitStatusTool statusTool,
            GitCommitTool commitTool,
            GitDiffTool diffTool,
            GitBranchListTool branchListTool,
            GitBranchCreateTool branchCreateTool,
            GitCheckoutTool checkoutTool,
            GitLogTool logTool,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        register("git_status", "Get the current git status", GitStatusToolSchema.class, statusTool::execute);
        register("git_commit", "Commit changes to the repository", GitCommitToolSchema.class, commitTool::execute);
        register("git_diff", "Show changes between commits, commit and working tree, etc", GitDiffToolSchema.class,
                diffTool::execute);
        register("git_branch_list", "List git branches", GitBranchListToolSchema.class, branchListTool::execute);
        register("git_branch_create", "Create a new git branch", GitBranchCreateToolSchema.class,
                branchCreateTool::execute);
        register("git_checkout", "Checkout a git branch", GitCheckoutToolSchema.class, checkoutTool::execute);
        register("git_log", "Show commit logs", GitLogToolSchema.class, logTool::execute);
    }

    public List<ToolDefinition> listTools() {
        return new ArrayList<>(toolDefinitions);
    }

    public ToolResult callTool(String name, Map<String, Object> arguments) {
        ToolHandler handler = toolHandlers.get(name);
        if (handler == null) {
            return ToolResult.error("Tool not found: " + name);
        }
        return handler.execute(arguments);
    }

    private <T> void register(String name, String description, Class<T> schemaClass, Function<T, ToolResult> executor) {
        // Create simple schema definition (placeholder for full JSON schema generation)
        // In a real generic implementation, we would generate JSON schema from the
        // class.
        // For now, passing a simple string representation or a manual map.
        JsonSchema schema = generateSimpleSchema(schemaClass);

        ToolDefinition definition = new ToolDefinition(name, description, schema);
        toolDefinitions.add(definition);

        toolHandlers.put(name, args -> {
            try {
                T input = objectMapper.convertValue(args, schemaClass);
                return executor.apply(input);
            } catch (Exception e) {
                return ToolResult.error("Invalid arguments: " + e.getMessage());
            }
        });
    }

    private JsonSchema generateSimpleSchema(Class<?> schemaClass) {
        // TODO: Implement proper JSON schema generation
        return JsonSchema.objectSchema(java.util.Collections.emptyMap());
    }

    @FunctionalInterface
    private interface ToolHandler {
        ToolResult execute(Map<String, Object> args);
    }
}
