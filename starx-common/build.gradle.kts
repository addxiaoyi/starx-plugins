plugins {
    id("java-library")
    id("starx.java-conventions")
}

dependencies {
    api(project(":starx-api"))

    implementation(libs.gson)
    implementation(libs.configurate.yaml)
    implementation(libs.bundles.database)
    implementation(libs.bcrypt)
    implementation(libs.totp)

    // 只保留 SQLite 作为默认本地数据库
    // 需要外置数据库的用户可以自己提供驱动
    runtimeOnly(libs.sqlite)

    testImplementation(project(":starx-testfixtures"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testImplementation(libs.h2)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgres)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    // 项目路径包含非 ASCII 字符，Gradle 在 Windows 上以 GBK 写出 worker classpath
    // argfile，而 worker JVM 默认使用 UTF-8 读取，导致中文/特殊符号路径失效。
    jvmArgs("-Dfile.encoding=GBK")
}
