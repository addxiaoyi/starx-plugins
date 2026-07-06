import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.addxiaoyi"
    version = (project.findProperty("version") as? String) ?: "0.1.5"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.elytrium.xyz/snapshots/")
        maven("https://repo.elytrium.xyz/releases/")
        maven("https://repo.skinsrestorer.net/repository/maven-public/")
    }
}

tasks.register("downloadLimboAPI") {
    group = "limboapi"
    description = "下载最新版本的 LimboAPI 插件"

    doLast {
        val outputDir = file("limboapi-plugins")
        outputDir.mkdirs()

        // GitHub API URL to get latest dev-build
        val apiUri = URI.create("https://api.github.com/repos/Elytrium/LimboAPI/releases/tags/dev-build")
        val connection = apiUri.toURL().openConnection()
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        
        // 简单解析 JSON（避免额外依赖）
        val assetUrlPattern = """"browser_download_url":\s*"([^"]+)"""".toRegex()
        val match = assetUrlPattern.find(response)

        if (match != null) {
            val downloadUrl = match.groupValues[1]
            val fileName = downloadUrl.substringAfterLast("/")
            val outputFile = File(outputDir, fileName)

            println("正在下载 LimboAPI 从 $downloadUrl")
            
            URI.create(downloadUrl).toURL().openStream().use { input ->
                Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            
            println("下载完成！已保存到: ${outputFile.absolutePath}")
            println("\n请将此文件复制到你的 Velocity 服务器的 plugins/ 目录中")
        } else {
            println("错误：无法找到 LimboAPI 的下载地址")
            println("请手动访问 https://github.com/Elytrium/LimboAPI/releases 下载")
        }
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
    format("markdown") {
        target("docs/**/*.md", "obsidian-vault/**/*.md", "README.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
