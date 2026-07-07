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
    // 只保留 SQLite，这是默认本地数据库
    // 需要外置数据库的用户可以自己提供驱动
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
    // SQLite 不 relocate，因为它包含原生库
    // 如果 relocate，它会找不到自己的原生库文件
    
    // SQLite: 只保留最常用的平台（Linux/Windows x86_64 + aarch64）
    exclude("org/sqlite/native/Linux-Android/**")
    exclude("org/sqlite/native/Linux-Musl/**")
    exclude("org/sqlite/native/Linux/arm/**")
    exclude("org/sqlite/native/Linux/armv6/**")
    exclude("org/sqlite/native/Linux/armv7/**")
    exclude("org/sqlite/native/Linux/x86/**")
    exclude("org/sqlite/native/Linux/ppc64/**")
    exclude("org/sqlite/native/Linux/riscv64/**")
    exclude("org/sqlite/native/Mac/**")
    exclude("org/sqlite/native/FreeBSD/**")
    exclude("org/sqlite/native/Windows/arm/**")
    exclude("org/sqlite/native/Windows/armv7/**")
    exclude("org/sqlite/native/Windows/x86/**")
    
    // 排除不必要的 META-INF 文件
    exclude("META-INF/maven/**")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/versions/**")
    exclude("META-INF/*.kotlin_module")
    exclude("META-INF/*.version")
    exclude("module-info.class")
}
