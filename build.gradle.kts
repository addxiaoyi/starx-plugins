plugins {
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.addxiaoyi"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.elytrium.xyz/snapshots/")
        maven("https://repo.elytrium.xyz/releases/")
        maven("https://repo.skinsrestorer.net/repository/maven-public/")
    }
}

spotless {
    java {
        target("*/src/**/*.java")
        googleJavaFormat("1.25.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target("**/*.gradle.kts", "build-logic/src/**/*.kt")
        targetExclude("**/build/**/*.gradle.kts", "**/build/**/*.kt")
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("markdown") {
        target("docs/**/*.md", "obsidian-vault/**/*.md", "README.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
