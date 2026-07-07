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
    
    // Jackson — Paper 服务端自带，无需打包
    exclude("com/fasterxml/jackson/**")
    
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
    
    // 移除未使用的 shade 依赖类
    minimize {
        // 这些库通过 SPI 加载，minimize 不会追踪 SPI 引用
        exclude(dependency("org.xerial:sqlite-jdbc"))
        exclude(dependency("org.flywaydb:flyway-core"))
        exclude(dependency("com.zaxxer:HikariCP"))
        exclude(dependency("org.jdbi:jdbi3-core"))
        exclude(dependency("org.jdbi:jdbi3-sqlobject"))
    }
    
    // 排除不必要的 META-INF 文件
    exclude("META-INF/maven/**")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/versions/**")
    exclude("META-INF/*.kotlin_module")
    exclude("META-INF/*.version")
    exclude("module-info.class")
}
