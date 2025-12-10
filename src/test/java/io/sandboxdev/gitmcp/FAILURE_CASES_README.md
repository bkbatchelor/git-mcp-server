# Commit Metadata Completeness Property Test - Failure Cases

This document describes the failure cases created to demonstrate what kinds of bugs the property test for task 4.3 is designed to catch.

## Overview

The property test `CommitMetadataCompletenessProperty` validates **Property 5: Commit info contains complete metadata** from the design document, which ensures that commit information returned by the Git MCP Server contains all required metadata fields as specified in Requirements 2.2 and 2.4.

## Failure Cases

### Failure Case 1: Null Hash Bug (`CommitMetadataCompletenessFailureCase1`)

**Bug Simulation**: The system attempts to create commit objects with `null` hash fields, but proper validation should detect this and throw an exception.

**Expected Behavior**: 
- `getHistory()` throws `GitMcpException` with message containing "Invalid commit data" and "hash cannot be null"
- `getCommitDetails()` throws `GitMcpException` with message containing "Invalid commit data" and "hash cannot be null"

**Real-world Scenario**: This could happen if there's an issue with JGit integration where the commit object doesn't properly provide hash information, or if there's a null pointer exception that gets caught. The system should detect this invalid state and fail gracefully with a meaningful error message.

**Code Location**: `BuggyGitRepositoryServiceNullHash.validateCommitInfo()`

### Failure Case 2: Invalid Author Information Bug (`CommitMetadataCompletenessFailureCase2`)

**Bug Simulation**: The system attempts to create commit objects with empty author name and invalid email format, but proper validation should detect this and throw an exception.

**Expected Behavior**:
- `getHistory()` throws `GitMcpException` with message containing "Invalid commit data" and "author"
- `getCommitDetails()` throws `GitMcpException` with message containing "Invalid commit data" and "author"

**Real-world Scenario**: This could occur when parsing commits from repositories with malformed author information, or when there are encoding issues with author data. The system should validate author information and fail gracefully with a meaningful error message.

**Code Location**: `BuggyGitRepositoryServiceInvalidAuthor.validateCommitInfo()`

### Failure Case 3: Invalid Hash Format Bug (`CommitMetadataCompletenessFailureCase3`)

**Bug Simulation**: The system attempts to create commit objects with malformed hashes (wrong length, invalid characters), but proper validation should detect this and throw an exception.

**Expected Behavior**:
- `getHistory()` throws `GitMcpException` with message containing "Invalid commit data" and "hash format"
- `getCommitDetails()` throws `GitMcpException` with message containing "Invalid commit data" and "hash format"

**Real-world Scenario**: This could happen if there's a bug in hash generation/retrieval logic, or if the system is incorrectly processing commit identifiers. The system should validate hash format and consistency, failing gracefully with a meaningful error message.

**Code Location**: `BuggyGitRepositoryServiceInvalidHashFormat.validateCommitInfo()`

### Failure Case 4: Invalid Timestamp Bug (`CommitMetadataCompletenessFailureCase4`)

**Bug Simulation**: 
- `getHistory()` returns commits with future timestamps (year 2030)
- `getCommitDetails()` returns commits with `null` timestamps

**Expected Failure**:
- `assertThat(commit.timestamp()).isNotNull()` fails for `getCommitDetails()`
- `assertThat(commit.timestamp()).isBefore(Instant.now().plusSeconds(60))` fails for `getHistory()` with future timestamp

**Real-world Scenario**: This could occur due to timezone handling bugs, system clock issues, or problems parsing timestamp data from Git objects.

**Code Location**: `BuggyGitRepositoryServiceInvalidTimestamp.getHistory()` and `getCommitDetails()`

### Failure Case 5: Inconsistent Data Between Methods Bug (`CommitMetadataCompletenessFailureCase5`)

**Bug Simulation**: `getCommitDetails()` returns a different hash than what was requested, breaking consistency with `getHistory()`.

**Expected Failure**:
- `assertThat(commitDetails.hash()).isEqualTo(firstCommitHash)` fails because the method returns a hardcoded different hash

**Real-world Scenario**: This could happen if there are caching issues, race conditions, or bugs in the repository management layer that cause different methods to return inconsistent data for the same commit.

**Code Location**: `BuggyGitRepositoryServiceInconsistentData.getCommitDetails()`

## How to Run Failure Cases

**NOTE**: These test classes are designed to test proper exception handling and should pass when the system correctly validates commit data.

To run a specific failure case:

```bash
# Run a specific failure case (this should pass by testing exception throwing)
mvn test -Dtest=CommitMetadataCompletenessFailureCase1

# Run all failure cases
mvn test -Dtest="*CommitMetadataCompletenessFailureCase*"
```

## Verification Through Logging

Each failure case includes comprehensive logging to help verify that the validation is working correctly:

### Log Levels Used:
- **INFO**: High-level test progress and success confirmations
- **DEBUG**: Detailed validation steps and data being validated  
- **WARN**: Validation failures with specific details about what was invalid

### Example Log Output:
```
INFO  - FAILURE CASE 1: Testing null hash validation for repository: /tmp/git-test-123
DEBUG - Testing getHistory() with null hash validation
DEBUG - Validating commit info: hash=null, shortHash=null
WARN  - VALIDATION FAILED: Detected null hash in commit data
INFO  - ✓ getHistory() correctly threw GitMcpException for null hash
INFO  - FAILURE CASE 1: PASSED - System correctly validates null hashes
```

### Viewing Logs:
- **Application logs**: Check `logs/git-mcp-server.log` for detailed validation logging
- **Test output**: Surefire reports show test execution results
- **Real-time**: Run tests with `-X` flag for verbose Maven output

### What the Logging Verifies:

1. **Test Execution Flow**: Confirms each failure case runs and tests the expected validation
2. **Validation Logic**: Shows exactly what data is being validated and why it fails
3. **Exception Throwing**: Confirms that `GitMcpException` is thrown with correct error messages
4. **Data Inspection**: Reveals the actual invalid data being detected (null values, malformed hashes, etc.)
5. **Property-Based Testing**: Shows multiple test iterations with different generated data

### Logging Benefits for Debugging:

- **Immediate Feedback**: See exactly what validation failed and why
- **Data Visibility**: Inspect the actual invalid data that triggered validation failures
- **Flow Tracing**: Follow the complete test execution from start to finish
- **Regression Detection**: Logs help identify if validation logic changes unexpectedly

## Exception-Based Validation

These failure cases demonstrate that the system should properly validate commit data and throw meaningful exceptions when invalid data is detected:

1. **Null Safety**: System throws `GitMcpException` when required fields are null
2. **Format Validation**: System throws `GitMcpException` when hashes are not valid SHA-1 format (40 hex characters)
3. **Data Consistency**: System throws `GitMcpException` when short hash is not prefix of full hash
4. **Email Validation**: System throws `GitMcpException` when author email is invalid (missing '@' symbol)
5. **Timestamp Bounds**: System throws `GitMcpException` when timestamps are unreasonable
6. **Method Consistency**: System throws `GitMcpException` when data inconsistencies are detected
7. **Graceful Failure**: All validation failures result in meaningful error messages rather than returning invalid data

## Benefits of Property-Based Testing

These failure cases illustrate why property-based testing is valuable:

- **Comprehensive Coverage**: Tests many different input combinations automatically
- **Edge Case Discovery**: Finds bugs that might not be caught by example-based tests
- **Specification Validation**: Ensures the implementation truly meets the requirements
- **Regression Prevention**: Catches bugs introduced by future changes
- **Documentation**: The property test serves as executable specification

## Integration with Requirements

Each failure case maps back to specific requirements:

- **Requirements 2.2**: "WHEN the AI Assistant requests commit history, THE Git MCP Server SHALL return a list of commits with author, timestamp, message, and commit hash"
- **Requirements 2.4**: "WHEN the AI Assistant requests details for a specific commit, THE Git MCP Server SHALL return the full commit information including changed files and diff statistics"

The property test ensures these requirements are met by validating that all specified metadata is present, complete, and correctly formatted.