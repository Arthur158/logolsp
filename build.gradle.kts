import org.gradle.api.plugins.antlr.AntlrTask
// import io.github.goooler.shadow.tasks.ShadowJar

plugins {
    java
    antlr
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "logo"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")

    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")

    // Logging — LSP servers should log to stderr, not stdout (stdout is the JSON-RPC channel)
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<AntlrTask>("generateGrammarSource") {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "logo.parser", "-visitor", "-no-listener")
    outputDirectory = file("build/generated-src/antlr/main/logo/parser")
}

// Make sure generated ANTLR sources are on the compile classpath
sourceSets {
    main {
        java {
            srcDir("build/generated-src/antlr/main")
        }
    }
}

// Fat JAR — bundles everything including ANTLR runtime and LSP4J
tasks.shadowJar {
    archiveBaseName = "logo-lsp"
    archiveClassifier = ""
    archiveVersion = ""
    manifest {
        attributes["Main-Class"] = "logo.Main"
    }
}

tasks.test {
    useJUnitPlatform()
}

// Running `./gradlew run` starts the server on stdio for quick testing
tasks.register<JavaExec>("run") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "logo.Main"
    // Redirect stderr to a file so it doesn't interfere with stdio JSON-RPC
    standardInput = System.`in`
    errorOutput = System.err
}
