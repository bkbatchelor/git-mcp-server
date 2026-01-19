# Implementation Plan: Replace ProcessBuilder with JGit

## Phase 1: Infrastructure and Setup [checkpoint: ecc9dc9]
- [x] Task: Add JGit dependency to `build.gradle.kts` 9d2419b
- [x] Task: Configure File-Based Logging 0f204c8
    - [x] Update `logback-spring.xml` to include a `RollingFileAppender`.
    - [x] Configure log directory (defaulting to current working dir or `logs/`).
    - [x] Verify logs are written to file and NOT `stdout`.
- [x] Task: Update `GitService` to manage JGit `Git` and `Repository` instances 88be69d
- [x] Task: Conductor - User Manual Verification 'Infrastructure and Setup' (Protocol in workflow.md) ecc9dc9

## Phase 2: Migrate Read Operations [checkpoint: 4b0698a]
- [x] Task: Migrate `list_branches` to JGit 88be69d
    - [x] Write failing test in `GitServiceTests` for JGit-based branch listing
    - [x] Implement JGit logic in `GitService.listBranches()`
    - [x] Verify test passes
- [x] Task: Migrate repository status/validation checks to JGit d3a231d
    - [x] Write failing tests for repository validation
    - [x] Implement JGit-based validation
    - [x] Verify tests pass
- [x] Task: Conductor - User Manual Verification 'Migrate Read Operations' (Protocol in workflow.md) 4b0698a

## Phase 3: Migrate Write Operations
- [x] Task: Migrate `checkout_branch` to JGit 3f75910
    - [x] Write failing test for branch checkout
    - [x] Implement JGit `checkout()` command
    - [x] Verify test passes
- [x] Task: Migrate staging and commit operations to JGit 922bbaa
    - [x] Write failing tests for add and commit
    - [x] Implement JGit `add()` and `commit()` commands
    - [x] Verify tests pass
- [ ] Task: Conductor - User Manual Verification 'Migrate Write Operations' (Protocol in workflow.md)

## Phase 4: Cleanup and Finalization
- [ ] Task: Remove `ProcessBuilder` and shell-specific utility methods from `GitService`
- [ ] Task: Perform final integration verification with `ToolDiscoveryTests` and manual client tests
- [ ] Task: Conductor - User Manual Verification 'Cleanup and Finalization' (Protocol in workflow.md)
