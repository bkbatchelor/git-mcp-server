package io.sandboxdev.gitmcpserver.git;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {
    private final Path workingDir;

    public GitService() {
        this.workingDir = Path.of(".");
    }

    public GitService(Path workingDir) {
        this.workingDir = workingDir;
    }

    public List<String> listBranches() {
        List<String> branches = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "branch", "--format=%(refname:short)");
            pb.directory(workingDir.toFile());
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    branches.add(line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list branches", e);
        }
        return branches;
    }
}
