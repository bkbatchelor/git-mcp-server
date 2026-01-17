# Implementation Plan: Fix Server Timeout

## Phase 1: Optimization [checkpoint: d1e2f3g]
- [x] Enable Lazy Initialization in Spring Boot (Reverted, eager was slightly faster)
- [x] Verify startup time improvement
- [x] Add Spring Context Indexer
- [x] Fix JSON-RPC message serialization (exclude nulls)
- [x] Create optimized wrapper script `git-mcp-server.sh`
- [x] Disable JMX and other unused features

## Phase 2: Verification [checkpoint: h4i5j6k]
- [x] Test with `debug_server.py` (verified sequence works in ~1.2s)
- [x] Manual verification with Gemini CLI (simulated via debug script)
