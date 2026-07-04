plugins {
    id("java-library")
    id("starx.java-conventions")
}

dependencies {
    // API module is intentionally lightweight; no platform dependencies.
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.launcher)
}
