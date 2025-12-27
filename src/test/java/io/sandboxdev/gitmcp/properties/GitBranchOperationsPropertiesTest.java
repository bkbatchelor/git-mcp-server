package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitBranchCreateToolSchema;
import io.sandboxdev.gitmcp.model.GitBranchInfo;
import io.sandboxdev.gitmcp.model.GitBranchListToolSchema;
import io.sandboxdev.gitmcp.model.GitCheckoutToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.GitBranchCreateTool;
import io.sandboxdev.gitmcp.tools.GitBranchListTool;
import io.sandboxdev.gitmcp.tools.GitCheckoutTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("git-mcp-server")
@Tag("git-branch-operations")
class GitBranchOperationsPropertiesTest {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @RepeatedTest(10)
        @DisplayName("Property 7: Git Branch Operations - List, create, and checkout branches workflow (Req 6.1, 6.2, 6.3, 6.4)")
        void gitBranchOperationsWorkflow() throws Exception {
                // Generate random data
                Path repositoryPath = createInitializedRepository();
                String newBranchName = "feature-" + UUID.randomUUID().toString().substring(0, 8);

                // Tools
                GitBranchListTool listTool = new GitBranchListTool();
                GitBranchCreateTool createTool = new GitBranchCreateTool();
                GitCheckoutTool checkoutTool = new GitCheckoutTool();

                // 1. List branches - should have at least one (master/main) and it should be
                // current
                ToolResult listResult = listTool.execute(new GitBranchListToolSchema(repositoryPath.toString()));
                assertThat(listResult.isError()).isFalse();
                List<GitBranchInfo> initialBranches = objectMapper.convertValue(
                                listResult.content(),
                                new TypeReference<List<GitBranchInfo>>() {
                                });
                assertThat(initialBranches).isNotEmpty();
                assertThat(initialBranches).anyMatch(GitBranchInfo::isCurrent);

                // 2. Create new branch
                ToolResult createResult = createTool.execute(
                                new GitBranchCreateToolSchema(repositoryPath.toString(), newBranchName));
                assertThat(createResult.isError()).isFalse();

                // 3. List again - should see new branch
                listResult = listTool.execute(new GitBranchListToolSchema(repositoryPath.toString()));
                List<GitBranchInfo> branchesAfterCreate = objectMapper.convertValue(
                                listResult.content(),
                                new TypeReference<List<GitBranchInfo>>() {
                                });
                assertThat(branchesAfterCreate).extracting(GitBranchInfo::name)
                                .contains(newBranchName);

                // 4. Checkout new branch
                ToolResult checkoutResult = checkoutTool.execute(
                                new GitCheckoutToolSchema(repositoryPath.toString(), newBranchName));
                assertThat(checkoutResult.isError()).isFalse();

                // 5. Verify current branch is now the new one
                listResult = listTool.execute(new GitBranchListToolSchema(repositoryPath.toString()));
                List<GitBranchInfo> branchesAfterCheckout = objectMapper.convertValue(
                                listResult.content(),
                                new TypeReference<List<GitBranchInfo>>() {
                                });

                GitBranchInfo currentBranch = branchesAfterCheckout.stream()
                                .filter(GitBranchInfo::isCurrent)
                                .findFirst()
                                .orElseThrow();

                assertThat(currentBranch.name()).isEqualTo(newBranchName);
        }

        private Path createInitializedRepository() throws Exception {
                Path tempDir = Files.createTempDirectory("git-branch-test");
                // Initialize git repository with one commit so we have a valid HEAD
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Initial commit
                Files.writeString(tempDir.resolve("README.md"), "# Initial");
                pb = new ProcessBuilder("git", "add", "README.md");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.name", "Test User");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "commit", "-m", "Initial commit");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                return tempDir;
        }
}
