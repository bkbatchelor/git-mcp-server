package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.error.ErrorHandler;
import io.sandboxdev.gitmcp.model.McpError;
import io.sandboxdev.gitmcp.model.ToolResult;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for error handling and graceful degradation.
 * Tests Property 11: Error Handling (Requirements 10.1, 10.2, 10.3, 10.4, 10.5)
 */
class ErrorHandlingProperties {

    private final ErrorHandler errorHandler = new ErrorHandler(new ObjectMapper());

    /**
     Property 11: Error Handling (Req 10.1)
     Git operation failures return human-readable error messages
     */
    @Property
    void gitOperationFailuresReturnHumanReadableErrors(@ForAll("gitOperationFailures") GitOperationFailure failure) {
        // This test should fail initially - we need to implement proper error handling
        ToolResult result = handleGitOperationFailure(failure);
        
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).isNotBlank();
        assertThat(result.errorMessage().get()).doesNotContain("Exception");
        assertThat(result.errorMessage().get()).doesNotContain("Stack trace");
    }

    /**
     Property 11: Error Handling (Req 10.2)
     Exceptions are translated to JSON-RPC errors without stack traces
     */
    @Property
    void exceptionsTranslatedToJsonRpcErrors(@ForAll("javaExceptions") Exception exception) {
        // This test should fail initially - we need to implement exception translation
        McpError mcpError = translateExceptionToMcpError(exception);
        
        assertThat(mcpError.code()).isEqualTo(-32603); // Internal Error
        assertThat(mcpError.message()).isNotBlank();
        assertThat(mcpError.message()).doesNotContain("at ");
        assertThat(mcpError.message()).doesNotContain(".java:");
        assertThat(mcpError.data()).isNotNull();
    }

    /**
     Property 11: Error Handling (Req 10.3)
     Invalid repository states provide descriptive errors with corrective actions
     */
    @Property
    void invalidRepositoryStatesProvideCorrectiveActions(@ForAll("invalidRepositoryStates") InvalidRepositoryState state) {
        // This test should fail initially - we need to implement descriptive error handling
        ToolResult result = handleInvalidRepositoryState(state);
        
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).containsIgnoringCase("suggestion");
        String message = result.errorMessage().get().toLowerCase();
        assertThat(message).containsAnyOf("try", "check", "verify", "ensure");
    }

    /**
     Property 11: Error Handling (Req 10.4)
     Internal errors use correct JSON-RPC error code
     */
    @Property
    void internalErrorsUseCorrectErrorCode(@ForAll("internalErrors") InternalError error) {
        // This test should fail initially - we need to implement proper error codes
        McpError mcpError = handleInternalError(error);
        
        assertThat(mcpError.code()).isEqualTo(-32603);
        assertThat(mcpError.message()).isNotBlank();
    }

    /**
     Property 11: Error Handling (Req 10.5)
     Tool timeouts return appropriate error messages
     */
    @Property
    void toolTimeoutsReturnAppropriateErrors(@ForAll("timeoutScenarios") TimeoutScenario timeout) {
        // This test should fail initially - we need to implement timeout handling
        ToolResult result = handleToolTimeout(timeout);
        
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).containsAnyOf("timeout", "exceeded", "time limit");
    }

    // Generators for property-based testing

    @Provide
    Arbitrary<GitOperationFailure> gitOperationFailures() {
        return Arbitraries.oneOf(
                Arbitraries.just(new GitOperationFailure("REPOSITORY_NOT_FOUND", "/invalid/path")),
                Arbitraries.just(new GitOperationFailure("PERMISSION_DENIED", "/restricted/repo")),
                Arbitraries.just(new GitOperationFailure("INVALID_REPOSITORY_STATE", "detached HEAD")),
                Arbitraries.just(new GitOperationFailure("MERGE_CONFLICT", "conflicting changes")),
                Arbitraries.just(new GitOperationFailure("NO_STAGED_CHANGES", "nothing to commit"))
        );
    }

    @Provide
    Arbitrary<Exception> javaExceptions() {
        return Arbitraries.oneOf(
                Arbitraries.just(new RuntimeException("Test runtime exception")),
                Arbitraries.just(new IllegalArgumentException("Invalid argument")),
                Arbitraries.just(new IllegalStateException("Invalid state")),
                Arbitraries.just(new NullPointerException("Null pointer")),
                Arbitraries.just(new UnsupportedOperationException("Not supported"))
        );
    }

    @Provide
    Arbitrary<InvalidRepositoryState> invalidRepositoryStates() {
        return Arbitraries.oneOf(
                Arbitraries.just(new InvalidRepositoryState("DETACHED_HEAD", "HEAD is detached")),
                Arbitraries.just(new InvalidRepositoryState("MERGE_IN_PROGRESS", "Merge in progress")),
                Arbitraries.just(new InvalidRepositoryState("REBASE_IN_PROGRESS", "Rebase in progress")),
                Arbitraries.just(new InvalidRepositoryState("CORRUPTED_INDEX", "Index is corrupted"))
        );
    }

    @Provide
    Arbitrary<InternalError> internalErrors() {
        return Arbitraries.oneOf(
                Arbitraries.just(new InternalError("SYSTEM_ERROR", "System failure")),
                Arbitraries.just(new InternalError("MEMORY_ERROR", "Out of memory")),
                Arbitraries.just(new InternalError("IO_ERROR", "I/O operation failed"))
        );
    }

    @Provide
    Arbitrary<TimeoutScenario> timeoutScenarios() {
        return Arbitraries.oneOf(
                Arbitraries.just(new TimeoutScenario("git_status", 30000)),
                Arbitraries.just(new TimeoutScenario("git_commit", 60000)),
                Arbitraries.just(new TimeoutScenario("git_diff", 45000))
        );
    }

    // Helper methods that now use ErrorHandler (GREEN phase)
    
    private ToolResult handleGitOperationFailure(GitOperationFailure failure) {
        return errorHandler.handleGitOperationFailure(failure.type(), failure.details());
    }

    private McpError translateExceptionToMcpError(Exception exception) {
        return errorHandler.translateExceptionToMcpError(exception);
    }

    private ToolResult handleInvalidRepositoryState(InvalidRepositoryState state) {
        return errorHandler.handleInvalidRepositoryState(state.state(), state.description());
    }

    private McpError handleInternalError(InternalError error) {
        return errorHandler.handleInternalError(error.type(), error.message());
    }

    private ToolResult handleToolTimeout(TimeoutScenario timeout) {
        return errorHandler.handleToolTimeout(timeout.operation(), timeout.timeoutMs());
    }

    // Test data records
    public record GitOperationFailure(String type, String details) {}
    public record InvalidRepositoryState(String state, String description) {}
    public record InternalError(String type, String message) {}
    public record TimeoutScenario(String operation, long timeoutMs) {}
}
