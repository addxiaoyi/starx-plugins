plugins {
    base
}

val snapshotJarName = "limboapi-1.1.27-SNAPSHOT.jar"
val officialJarName = "limboapi-1.1.26-official.jar"
val patchedJarName = "limboapi-1.1.27-SNAPSHOT-velocity-3.5-patched.jar"

val snapshotJar = file(snapshotJarName)
val officialJar = file(officialJarName)
val localMappingDir = file("mapping")

val patchedJar = layout.buildDirectory.file("libs/$patchedJarName").map { it.asFile }

tasks.register("buildPatchedLimboAPI") {
    group = "limboapi"
    description = "构建修复后的 LimboAPI JAR（合并 1.1.26 的完整 mapping 资源 + 1.1.27 的新 class）"

    inputs.file(snapshotJar)
    inputs.file(officialJar)
    inputs.dir(localMappingDir)
    outputs.file(patchedJar)

    doLast {
        val output = patchedJar.get()
        output.parentFile.mkdirs()

        // 先用 1.1.27-SNAPSHOT 的内容（新 class），跳过它原有的不完整 mapping
        val snapshotZip = java.util.zip.ZipFile(snapshotJar)
        val officialZip = java.util.zip.ZipFile(officialJar)

        val fos = java.io.FileOutputStream(output)
        val zos = java.util.zip.ZipOutputStream(fos)

        // 已写入的条目追踪（避免重复）
        val written = mutableSetOf<String>()

        // 写入 1.1.27-SNAPSHOT 的非 mapping 条目（class 文件等）
        snapshotZip.entries().asSequence().forEach { entry ->
            if (entry.name.startsWith("META-INF/")) return@forEach
            if (entry.name.startsWith("mapping/")) return@forEach
            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
            snapshotZip.getInputStream(entry).transferTo(zos)
            zos.closeEntry()
            written.add(entry.name)
        }

        // 从 1.1.26-official 写入所有 mapping 文件
        officialZip.entries().asSequence().forEach { entry ->
            if (!entry.name.startsWith("mapping/")) return@forEach
            if (entry.name in written) return@forEach
            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
            officialZip.getInputStream(entry).transferTo(zos)
            zos.closeEntry()
            written.add(entry.name)
        }

        // 写入本地 mapping 覆盖（data_component_types.json 等）
        if (localMappingDir.isDirectory) {
            localMappingDir.listFiles()?.forEach { file ->
                val entryName = "mapping/${file.name}"
                if (entryName in written) return@forEach
                zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                file.inputStream().transferTo(zos)
                zos.closeEntry()
                written.add(entryName)
            }
        }

        snapshotZip.close()
        officialZip.close()
        zos.close()
        fos.close()

        logger.lifecycle("Patched JAR built: ${output.absolutePath}")
    }
}

tasks.register<Delete>("cleanPatchedJar") {
    group = "limboapi"
    description = "删除构建的 patched JAR"
    delete(patchedJar)
}
