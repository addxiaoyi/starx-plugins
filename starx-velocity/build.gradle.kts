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
    // 包含所有数据库驱动，确保它们被打包到 shadowJar
    implementation(libs.h2)
    implementation(libs.mysql.connector)
    implementation(libs.postgresql)
    implementation(libs.sqlite)

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

val currentVersion = project.version

tasks.processResources {
    filesMatching("velocity-plugin.json") {
        expand("version" to currentVersion)
    }
}

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
    relocate("org.h2", "io.github.addxiaoyi.starx.libs.h2")
    relocate("com.mysql", "io.github.addxiaoyi.starx.libs.mysql")
    relocate("org.postgresql", "io.github.addxiaoyi.starx.libs.postgresql")
    relocate("org.sqlite", "io.github.addxiaoyi.starx.libs.sqlite")
}
