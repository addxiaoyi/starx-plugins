plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.aliyun.com/repository/public")
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.6")
}
