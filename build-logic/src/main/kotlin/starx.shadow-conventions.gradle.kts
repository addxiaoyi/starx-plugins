import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

tasks.withType<ShadowJar>().configureEach {
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.withType<ShadowJar>())
}
