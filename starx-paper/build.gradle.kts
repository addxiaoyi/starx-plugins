import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("starx.java-conventions")
    id("starx.shadow-conventions")
}

dependencies {
    api(project(":starx-common"))
    api(project(":starx-api"))
    compileOnly(libs.paper.api)
    implementation(libs.gson)
    implementation(libs.configurate.yaml)
    // 只保留 SQLite 作为默认本地数据库
    // 需要外置数据库的用户可以自己提供驱动
    implementation(libs.sqlite)

    testImplementation(project(":starx-testfixtures"))
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    val workerTmp = File("C:/tmp")
    workerTmp.mkdirs()
    systemProperty("java.io.tmpdir", workerTmp.absolutePath)
    val ts = System.currentTimeMillis()
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results-$ts/binary"))
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results-$ts/xml"))
}

val currentVersion = project.version

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to currentVersion)
    }
}

tasks.withType<ShadowJar>().configureEach {
    relocate("com.google.gson", "io.github.addxiaoyi.starx.libs.gson")
    relocate("org.flywaydb", "io.github.addxiaoyi.starx.libs.flyway")
    relocate("org.jdbi", "io.github.addxiaoyi.starx.libs.jdbi")
    relocate("com.zaxxer", "io.github.addxiaoyi.starx.libs.hikaricp")
    relocate("at.favre.lib.bytes", "io.github.addxiaoyi.starx.libs.bytes")
    relocate("at.favre.lib.hkdf", "io.github.addxiaoyi.starx.libs.hkdf")
    relocate("at.favre.lib.bcrypt", "io.github.addxiaoyi.starx.libs.bcrypt")
    // SQLite 不 relocate，因为它包含原生库
    // 如果 relocate，它会找不到自己的原生库文件
    
    // 只保留 Windows 平台的 SQLite 原生库，减小体积
    exclude("org/sqlite/native/Linux/**")
    exclude("org/sqlite/native/Linux-Android/**")
    exclude("org/sqlite/native/Linux-Musl/**")
    exclude("org/sqlite/native/Mac/**")
    exclude("org/sqlite/native/FreeBSD/**")
    exclude("org/sqlite/native/Windows/arm/**")
    exclude("org/sqlite/native/Windows/armv7/**")
    exclude("org/sqlite/native/Windows/x86/**")
    // 只保留 Windows x86_64 和 aarch64（覆盖大多数 Windows 用户）
    
    // 排除不必要的 META-INF 文件
    exclude("META-INF/maven/**")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/versions/**")
    exclude("META-INF/*.kotlin_module")
}
