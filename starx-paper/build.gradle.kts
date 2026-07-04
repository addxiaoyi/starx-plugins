import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("starx.java-conventions")
    id("starx.shadow-conventions")
}

dependencies {
    api(project(":starx-common"))
    compileOnly(libs.paper.api)
    implementation(libs.gson)
    implementation(libs.configurate.yaml)

    testImplementation(project(":starx-testfixtures"))
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<ShadowJar>().configureEach {
    relocate("com.google.gson", "io.github.addxiaoyi.starx.libs.gson")
}
