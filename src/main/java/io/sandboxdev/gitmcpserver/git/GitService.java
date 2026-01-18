package io.sandboxdev.gitmcpserver.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {
    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    private Path workingDir;

    @org.springframework.beans.factory.annotation.Autowired
    public GitService(ApplicationArguments args) {
        if (args != null && !args.getNonOptionArgs().isEmpty()) {
            this.workingDir = Path.of(args.getNonOptionArgs().get(0)).toAbsolutePath().normalize();
            log.info("Using repository path from arguments: {}", workingDir);
        } else {
            this.workingDir = Path.of(".").toAbsolutePath().normalize();
            log.info("No repository path provided, using current directory: {}", workingDir);
        }
    }

    public GitService(Path workingDir) {
        this.workingDir = workingDir.toAbsolutePath().normalize();
    }

    private Git getGit() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(workingDir.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
        if (!repository.getObjectDatabase().exists()) {
             throw new IOException("Invalid git repository: " + workingDir);
        }
        return new Git(repository);
    }

    public List<String> listBranches() {
        List<String> branches = new ArrayList<>();
        try (Git git = getGit()) {
            List<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                branches.add(Repository.shortenRefName(ref.getName()));
            }
        } catch (Exception e) {
            log.error("Failed to list branches using JGit", e);
            throw new RuntimeException("Failed to list branches", e);
        }
        return branches;
    }

    public List<java.util.Map<String, String>> getLog(int count) {
        List<java.util.Map<String, String>> log = new ArrayList<>();
        try {
            // format: hash|author|date|message
            ProcessBuilder pb = new ProcessBuilder("git", "log", "-n", String.valueOf(count), "--pretty=format:%H|%an|%ad|%s");
            pb.directory(workingDir.toFile());
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 4);
                    if (parts.length == 4) {
                        log.add(java.util.Map.of(
                            "hash", parts[0],
                            "author", parts[1],
                            "date", parts[2],
                            "message", parts[3]
                        ));
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get log", e);
        }
        return log;
    }
}
