package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import net.jqwik.api.*;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for MCP tool list completeness.
 */
@SpringBootTest
public class GitMcpToolListProperty {
    
    @Autowired
    private GitMcpToolProvider toolProvider;
    
    // Feature: git-mcp-server, Property 20: Tool list contains all Git operations
    @Property(tries = 100)
    void toolListContainsAllGitOperations(@ForAll("expectedToolName") String expectedTool) {
        // Given the expected set of Git operation tools
        // When we inspect the GitMcpToolProvider
        Set<String> actualTools = getToolNamesFromProvider();
        
        // Then all expected tools should be present
        assertThat(actualTools)
            .as("Tool list should contain all expected Git operations")
            .contains(expectedTool);
    }
    
    @Property(tries = 1)
    void allToolsHaveMcpToolAnnotation() {
        // Given the GitMcpToolProvider class
        Method[] methods = GitMcpToolProvider.class.getDeclaredMethods();
        
        // When we filter for public methods that should be tools
        Set<Method> publicMethods = Arrays.stream(methods)
            .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
            .filter(m -> !m.getName().equals("commitToMap")) // Exclude helper methods
            .collect(Collectors.toSet());
        
        // Then all public methods (except helpers) should have @McpTool annotation
        for (Method method : publicMethods) {
            assertThat(method.isAnnotationPresent(McpTool.class))
                .as("Method %s should have @McpTool annotation", method.getName())
                .isTrue();
        }
    }
    
    @Property(tries = 1)
    void toolListHasExpectedSize() {
        // Given the GitMcpToolProvider
        Set<String> actualTools = getToolNamesFromProvider();
        
        // Then we should have exactly 21 tools (as defined in requirements)
        // Repository: init, clone, getStatus, getHistory, getCommitDetails, getCurrentBranch (6)
        // Commit: stageFiles, unstageFiles, createCommit, getDiff, getFileContents (5)
        // Branch: createBranch, switchBranch, listBranches, deleteBranch (4)
        // Remote: push, pull, fetch, listRemotes, addRemote (5)
        // Message: generateCommitMessage (1)
        // Total: 21 tools
        assertThat(actualTools)
            .as("Tool list should contain exactly 21 Git operations")
            .hasSize(21);
    }
    
    @Property(tries = 1)
    void allToolsHaveDescriptions() {
        // Given the GitMcpToolProvider class
        Method[] methods = GitMcpToolProvider.class.getDeclaredMethods();
        
        // When we filter for methods with @McpTool annotation
        Set<Method> toolMethods = Arrays.stream(methods)
            .filter(m -> m.isAnnotationPresent(McpTool.class))
            .collect(Collectors.toSet());
        
        // Then all tools should have non-empty descriptions
        for (Method method : toolMethods) {
            McpTool annotation = method.getAnnotation(McpTool.class);
            assertThat(annotation.description())
                .as("Tool %s should have a description", method.getName())
                .isNotNull()
                .isNotEmpty();
        }
    }
    
    @Provide
    Arbitrary<String> expectedToolName() {
        return Arbitraries.of(
            // Repository operations (6)
            "initRepository",
            "cloneRepository",
            "getStatus",
            "getHistory",
            "getCommitDetails",
            "getCurrentBranch",
            
            // Commit operations (5)
            "stageFiles",
            "unstageFiles",
            "createCommit",
            "getDiff",
            "getFileContents",
            
            // Branch operations (4)
            "createBranch",
            "switchBranch",
            "listBranches",
            "deleteBranch",
            
            // Remote operations (5)
            "push",
            "pull",
            "fetch",
            "listRemotes",
            "addRemote",
            
            // Commit message generation (1)
            "generateCommitMessage"
        );
    }
    
    /**
     * Helper method to extract tool names from the GitMcpToolProvider.
     */
    private Set<String> getToolNamesFromProvider() {
        Method[] methods = GitMcpToolProvider.class.getDeclaredMethods();
        return Arrays.stream(methods)
            .filter(m -> m.isAnnotationPresent(McpTool.class))
            .map(Method::getName)
            .collect(Collectors.toSet());
    }
}
