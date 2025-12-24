plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.pitest)
    alias(libs.plugins.owasp.dependency.check)
    java
}

group = "io.sandboxdev"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    // Logging - explicit versions to prevent conflicts
    implementation(libs.logback.classic)
    implementation(libs.logback.core)

    // Spring Boot Core
    implementation(libs.bundles.spring.boot.core)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Spring AI
    implementation(libs.bundles.spring.ai)

    // Git Operations
    implementation(libs.jgit)

    // JSON Processing
    implementation(libs.jackson.datatype.jdk8)

    // Observability
    implementation(libs.bundles.observability)

    // Validation
    implementation(libs.jakarta.validation.api)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.testing.core)
    testImplementation(libs.bundles.testing.property)
    testImplementation(libs.bundles.testing.integration)
}

// Configure Java compilation
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Configure test execution
    maxHeapSize = "1g"
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // Enable strict stubbing for Mockito
    systemProperty("mockito.strictness", "strict_stubs")
}

// PiTest Configuration for Mutation Testing
pitest {
    targetClasses.set(listOf("io.sandboxdev.gitmcp.*"))
    excludedClasses.set(listOf(
        "io.sandboxdev.gitmcp.config.*",
        "io.sandboxdev.gitmcp.model.*",
        "io.sandboxdev.gitmcp.GitMcpServerApplication"
    ))
    threads.set(4)
    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
    mutationThreshold.set(80)
    testStrengthThreshold.set(85)
    verbose.set(true)
}

// OWASP Dependency Check Configuration
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
    analyzers {
        assemblyEnabled = false
        nuspecEnabled = false
        nodeEnabled = false
    }
}

// Spring Boot Configuration
springBoot {
    buildInfo()
}

// Enable Virtual Threads in Spring Boot
tasks.bootRun {
    systemProperty("spring.threads.virtual.enabled", "true")
}