package io.sandboxdev.gitmcp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.*;
import io.sandboxdev.gitmcp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GitToolRegistryTest {

    @Mock
    GitStatusTool statusTool;
    @Mock
    GitCommitTool commitTool;
    @Mock
    GitDiffTool diffTool;
    @Mock
    GitBranchListTool branchListTool;
    @Mock
    GitBranchCreateTool branchCreateTool;
    @Mock
    GitCheckoutTool checkoutTool;
    @Mock
    GitLogTool logTool;

    private ObjectMapper objectMapper = new ObjectMapper();
    private GitToolRegistry registry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registry = new GitToolRegistry(
                statusTool, commitTool, diffTool,
                branchListTool, branchCreateTool, checkoutTool, logTool,
                objectMapper);
    }

    @Test
    void listToolsReturnsAllRegisteredTools() {
        var tools = registry.listTools();
        assertThat(tools).hasSize(7);
        assertThat(tools).extracting("name")
                .contains("git_status", "git_commit", "git_diff",
                        "git_branch_list", "git_branch_create", "git_checkout", "git_log");
    }

    @Test
    void callToolDispatchesToCorrectHandler() {
        when(statusTool.execute(any())).thenReturn(ToolResult.success("status"));

        ToolResult result = registry.callTool("git_status", Map.of("repositoryPath", "."));

        assertThat(result.isError()).isFalse();
        verify(statusTool).execute(any(GitStatusToolSchema.class));
    }

    @Test
    void callToolReturnsErrorForUnknownTool() {
        ToolResult result = registry.callTool("unknown_tool", Collections.emptyMap());
        assertThat(result.isError()).isTrue();
        assertThat(result.content().asText()).contains("Tool not found");
    }
}
