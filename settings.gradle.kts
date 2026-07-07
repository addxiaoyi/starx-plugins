pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.elytrium.xyz/snapshots/")
        maven("https://repo.elytrium.xyz/releases/")
        maven("https://repo.skinsrestorer.net/repository/maven-public/")
        maven("https://repo.luckperms.net/")
    }
}

rootProject.name = "starx-plugins"

include("starx-api")
include("starx-common")
include("starx-testfixtures")
include("starx-velocity")
include("starx-paper")
include("limboapi-plugins")
