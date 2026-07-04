plugins {
    id("java-library")
    id("starx.java-conventions")
}

dependencies {
    api(project(":starx-api"))
    api(project(":starx-common"))

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.mockito.core)
    api(libs.mockito.junit)
    api(libs.assertj)
    api(libs.h2)
}
