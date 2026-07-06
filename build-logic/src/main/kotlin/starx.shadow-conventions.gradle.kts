import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.withType<ShadowJar>().configureEach {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.withType<ShadowJar>())
}
