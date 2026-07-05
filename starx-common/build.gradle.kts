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
    implementation(libs.micrometer.core)

    runtimeOnly(libs.h2)
    runtimeOnly(libs.mysql.connector)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.sqlite)

    testImplementation(project(":starx-testfixtures"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    // 项目路径包含非 ASCII 字符，Gradle 在 Windows 上以 GBK 写出 worker classpath
    // argfile，而 worker JVM 默认使用 UTF-8 读取，导致中文/特殊符号路径失效。
    jvmArgs("-Dfile.encoding=GBK")
}
