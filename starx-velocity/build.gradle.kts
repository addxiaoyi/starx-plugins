import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("starx.java-conventions")
    id("starx.shadow-conventions")
}

sourceSets {
    create("stubs") {
        java.srcDir("src/stubs/java")
    }
}

dependencies {
    api(project(":starx-common"))
    compileOnly(libs.velocity.api)
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    "stubsCompileOnly"(libs.velocity.api)
    implementation(libs.javalin)
    implementation(libs.micrometer.prometheus)

    testImplementation(project(":starx-testfixtures"))
    testImplementation(libs.velocity.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.database)
    testImplementation(libs.h2)
    testImplementation(libs.flyway.core)
    testRuntimeOnly(libs.junit.launcher)
}

sourceSets["main"].compileClasspath += sourceSets["stubs"].output.classesDirs

tasks.test {
    val workerTmp = File("C:/tmp")
    workerTmp.mkdirs()
    systemProperty("java.io.tmpdir", workerTmp.absolutePath)
    val ts = System.currentTimeMillis()
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results-$ts/binary"))
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results-$ts/xml"))
}

tasks.withType<ShadowJar>().configureEach {
    relocate("io.javalin", "io.github.addxiaoyi.starx.libs.javalin")
    relocate("com.google.gson", "io.github.addxiaoyi.starx.libs.gson")
    relocate("org.flywaydb", "io.github.addxiaoyi.starx.libs.flyway")
    relocate("org.jdbi", "io.github.addxiaoyi.starx.libs.jdbi")
    relocate("com.zaxxer", "io.github.addxiaoyi.starx.libs.hikaricp")
    relocate("at.favre.lib.bytes", "io.github.addxiaoyi.starx.libs.bytes")
    relocate("at.favre.lib.hkdf", "io.github.addxiaoyi.starx.libs.hkdf")
    relocate("at.favre.lib.bcrypt", "io.github.addxiaoyi.starx.libs.bcrypt")
}
