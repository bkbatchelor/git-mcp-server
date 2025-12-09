package io.sandboxdev.gitmcp.service.impl;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.*;
import io.sandboxdev.gitmcp.service.GitCommitService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@Service
public class GitCommitServiceImpl implements GitCommitService {
    private static final Logger logger = LoggerFactory.getLogger(GitCommitServiceImpl.class);
    
    private final JGitRepositoryManager repositoryManager;
    private final JGitCommandExecutor commandExecutor;
    
    public GitCommitServiceImpl(JGitRepositoryManager repositoryManager,
                               JGitCommandExecutor commandExecutor) {
        this.repositoryManager = repositoryManager;
        this.commandExecutor = commandExecutor;
    }

    
    @Override
    public void stageFiles(Path repositoryPath, List<String> filePaths) {
        logger.debug("Staging {} files in {}", filePaths.size(), repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                for (String filePath : filePaths) {
                    git.add().addFilepattern(filePath).call();
                }
                logger.info("Staged {} files", filePaths.size());
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "stage files");
        }
    }
    
    @Override
    public void unstageFiles(Path repositoryPath, List<String> filePaths) {
        logger.debug("Unstaging {} files in {}", filePaths.size(), repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                for (String filePath : filePaths) {
                    git.reset().addPath(filePath).call();
                }
                logger.info("Unstaged {} files", filePaths.size());
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "unstage files");
        }
    }

    
    @Override
    public CommitInfo createCommit(Path repositoryPath, String message, AuthorInfo author) {
        logger.debug("Creating commit in {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                
                // Check if there are staged changes
                if (!git.status().call().hasUncommittedChanges()) {
                    throw new GitMcpException(
                        ErrorCode.NOTHING_TO_COMMIT,
                        "No changes staged for commit"
                    );
                }
                
                var commitCommand = git.commit().setMessage(message);
                
                // Set author if provided
                if (author != null) {
                    commitCommand.setAuthor(author.name(), author.email());
                }
                
                RevCommit commit = commitCommand.call();
                
                logger.info("Created commit: {}", commit.getName());
                
                PersonIdent authorIdent = commit.getAuthorIdent();
                return new CommitInfo(
                    commit.getName(),
                    commit.abbreviate(7).name(),
                    commit.getFullMessage(),
                    new AuthorInfo(authorIdent.getName(), authorIdent.getEmailAddress()),
                    authorIdent.getWhenAsInstant(),
                    List.of(),
                    new DiffStats(0, 0, 0)
                );
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "create commit");
        }
    }

    
    @Override
    public String getDiff(Path repositoryPath, DiffType type, String... refs) {
        logger.debug("Getting diff of type {} in {}", type, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            try (Git git = new Git(repository);
                 DiffFormatter formatter = new DiffFormatter(outputStream)) {
                
                formatter.setRepository(repository);
                
                List<DiffEntry> diffs = switch (type) {
                    case UNSTAGED -> git.diff().call();
                    case STAGED -> git.diff().setCached(true).call();
                    case BETWEEN_COMMITS -> {
                        if (refs.length < 2) {
                            throw new GitMcpException(
                                ErrorCode.INVALID_PARAMETERS,
                                "BETWEEN_COMMITS requires two commit references"
                            );
                        }
                        ObjectId oldCommit = repository.resolve(refs[0]);
                        ObjectId newCommit = repository.resolve(refs[1]);
                        yield git.diff()
                            .setOldTree(prepareTreeParser(repository, oldCommit))
                            .setNewTree(prepareTreeParser(repository, newCommit))
                            .call();
                    }
                };
                
                for (DiffEntry diff : diffs) {
                    formatter.format(diff);
                }
                
                return outputStream.toString(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "get diff");
        }
    }

    
    @Override
    public String getFileContents(Path repositoryPath, String commitHash, String filePath) {
        logger.debug("Getting file contents: {} at commit {} in {}", 
            filePath, commitHash, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            
            ObjectId commitId = repository.resolve(commitHash);
            if (commitId == null) {
                throw new GitMcpException(
                    ErrorCode.INVALID_PARAMETERS,
                    "Invalid commit hash: " + commitHash
                );
            }
            
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitId);
                RevTree tree = commit.getTree();
                
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(filePath));
                    
                    if (!treeWalk.next()) {
                        throw new GitMcpException(
                            ErrorCode.FILE_NOT_FOUND,
                            "File not found: " + filePath + " at commit " + commitHash
                        );
                    }
                    
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    
                    return new String(loader.getBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "get file contents");
        }
    }

    
    /**
     * Helper method to prepare tree parser for diff operations.
     */
    private org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(
            Repository repository, ObjectId commitId) throws IOException {
        
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(commitId);
            RevTree tree = walk.parseTree(commit.getTree().getId());
            
            org.eclipse.jgit.treewalk.CanonicalTreeParser treeParser = 
                new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            
            walk.dispose();
            return treeParser;
        }
    }
}
