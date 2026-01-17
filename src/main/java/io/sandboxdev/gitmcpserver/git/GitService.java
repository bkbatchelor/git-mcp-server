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
