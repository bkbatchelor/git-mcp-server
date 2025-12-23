---
name: quality-gate
description: Enforces strict quality gate requirements for Gradle builds, including zero-tolerance test failures, 80% code and mutation coverage, and security scanning. Use when configuring build logic or validating compliance with EARS/INCOSE standards.
---

# Quality Gate Requirements 

## 1. Test Execution & Pass Rate
* **Test Lifecycle Mandate:** When the Gradle build lifecycle executes the `check` task, the Build System shall execute all defined Unit and Integration tests.
* **Zero Tolerance Policy:** If any Unit or Integration test fails, the Build System shall immediately fail the build process.
* **Execution Enforcement:** The Build System shall enforce a zero-tolerance policy for bypassed tests, rejecting any execution attempted with the `-x test` flag in the CI/CD environment.

## 2. Coverage & Robustness Metrics
* **Unit Test Coverage:** When measuring code execution, the Build System shall enforce a minimum **Unit Test Coverage of 80%** for all logic-heavy layers.
* **Mutation Analysis:** When validating code robustness, the Build System shall utilize **PiTest** to perform mutation testing on Services and Domain logic.
* **Mutation Coverage Threshold:** When calculating mutation metrics, the Build System shall fail if the **Mutation Coverage** is less than **80%**.
* **Test Strength Threshold:** When calculating mutation metrics, the Build System shall fail if the **Test Strength** is less than **85%**.
* **Analysis Scope:** The Build System shall exclude Data Transfer Objects (DTOs) and Configuration classes from the scope of coverage and mutation analysis.

## 3. Security Scanning
* **Vulnerability Scanning:** When assembling the application, the Build System shall execute the **OWASP Dependency Check** plugin to identify vulnerabilities in third-party libraries.
* **Severity Threshold:** If the OWASP Dependency Check detects a vulnerability with a severity of **High** or **Critical**, the Build System shall fail the build.

## 4. Configuration Standards
* **DSL Format:** The Build System shall define all quality gate configurations exclusively within the `build.gradle.kts` file using the **Kotlin DSL**.
* **Version Control:** The Build System shall retrieve all testing library versions strictly from the Version Catalog located at `gradle/libs.versions.toml`.