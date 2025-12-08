package io.sandboxdev.gitmcp.model;

/**
 * Type of diff to generate.
 */
public enum DiffType {
    /** Diff between working directory and staging area (unstaged changes) */
    UNSTAGED,
    
    /** Diff between staging area and last commit (staged changes) */
    STAGED,
    
    /** Diff between two commits */
    BETWEEN_COMMITS
}
