import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    base
    java
}

dependencies {
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")
}

val snapshotJarName = "limboapi-1.1.27-SNAPSHOT.jar"
val officialJarName = "limboapi-1.1.26-official.jar"
val patchedJarName = "limboapi-1.1.28-beta-patched.jar"
val mergedJarName = "limboapi-1.1.27-SNAPSHOT-merged.jar"
val noUpdateJarName = "limboapi-1.1.27-SNAPSHOT-noupdate.jar"

val snapshotJar = file(snapshotJarName)
val officialJar = file(officialJarName)
val localMappingDir = file("mapping")
val velocityCompatJar = file("../../velocity-test/velocity-3.4.0-566.jar")

val mergedJar = layout.buildDirectory.file("libs/$mergedJarName").map { it.asFile }
val noUpdateJar = layout.buildDirectory.file("libs/$noUpdateJarName").map { it.asFile }
val patchedJar = layout.buildDirectory.file("libs/$patchedJarName").map { it.asFile }

val mergeTask = tasks.register("mergeLimboAPIJars") {
    group = "limboapi"
    description = "合并 1.1.26 的完整 mapping 和 1.1.27 的新 class"

    inputs.file(snapshotJar)
    inputs.file(officialJar)
    inputs.dir(localMappingDir)
    outputs.file(mergedJar)

    doLast {
        val output = mergedJar.get()
        output.parentFile.mkdirs()

        val snapshotZip = ZipFile(snapshotJar)
        val officialZip = ZipFile(officialJar)

        val fos = FileOutputStream(output)
        val zos = ZipOutputStream(fos)

        val written = mutableSetOf<String>()

        // Write 1.1.27-SNAPSHOT non-mapping entries (class files etc.)
        val snapshotEnum = snapshotZip.entries()
        while (snapshotEnum.hasMoreElements()) {
            val entry = snapshotEnum.nextElement()
            if (entry.name.startsWith("META-INF/")) continue
            if (entry.name.startsWith("mapping/")) continue
            zos.putNextEntry(ZipEntry(entry.name))
            snapshotZip.getInputStream(entry).transferTo(zos)
            zos.closeEntry()
            written.add(entry.name)
        }

        // Write all mapping files from 1.1.26-official
        val officialEnum = officialZip.entries()
        while (officialEnum.hasMoreElements()) {
            val entry = officialEnum.nextElement()
            if (!entry.name.startsWith("mapping/")) continue
            if (entry.name in written) continue
            zos.putNextEntry(ZipEntry(entry.name))
            officialZip.getInputStream(entry).transferTo(zos)
            zos.closeEntry()
            written.add(entry.name)
        }

        // Write local mapping overrides (data_component_types.json etc.)
        val mappingFiles = localMappingDir.listFiles()
        if (mappingFiles != null) {
            for (file in mappingFiles) {
                val entryName = "mapping/${file.name}"
                if (entryName in written) continue
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().transferTo(zos)
                zos.closeEntry()
                written.add(entryName)
            }
        }

        snapshotZip.close()
        officialZip.close()
        zos.close()
        fos.close()

        logger.lifecycle("Merged JAR: ${output.absolutePath}")
    }
}

tasks.register("patchUpdateChecker", JavaExec::class) {
    dependsOn(mergeTask, "classes")
    group = "limboapi"
    description = "修补 UpdatesChecker 字节码，移除联网更新检查（避免启动超时）"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.github.addxiaoyi.starx.limboapi.patcher.UpdateCheckerPatcher"

    doFirst {
        args(mergedJar.get().canonicalPath, noUpdateJar.get().canonicalPath)
    }
}

tasks.register("patchBlockEntityVersion", JavaExec::class) {
    dependsOn("patchUpdateChecker")
    group = "limboapi"
    description = "智能修补 BlockEntityVersion 字节码以兼容 Velocity 3.4.x"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.github.addxiaoyi.starx.limboapi.patcher.BlockEntityVersionPatcher"

    doFirst {
        val compatJarPath = if (velocityCompatJar.exists()) {
            velocityCompatJar.canonicalPath
        } else {
            logger.warn("Velocity compat jar not found at ${velocityCompatJar.canonicalPath}, using fallback mode")
            "none"
        }
        args(compatJarPath, noUpdateJar.get().canonicalPath, patchedJar.get().canonicalPath)
    }
}

tasks.register("buildPatchedLimboAPI") {
    group = "limboapi"
    description = "构建智能修补后的 LimboAPI JAR (v1.1.28-beta)"
    dependsOn("patchBlockEntityVersion")
}

tasks.register<Delete>("cleanPatchedJar") {
    group = "limboapi"
    description = "删除构建的 patched JAR"
    delete(patchedJar, mergedJar, noUpdateJar)
}
