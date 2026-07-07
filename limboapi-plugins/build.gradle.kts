plugins {
    base
}

val baseJarName = "limboapi-1.1.27-SNAPSHOT.jar"
val patchedJarName = "limboapi-1.1.27-SNAPSHOT-velocity-3.5-patched.jar"

val baseJar = file(baseJarName)
val mappingDir = file("mapping")

val patchedJar = layout.buildDirectory.file("libs/$patchedJarName").map { it.asFile }

tasks.register("buildPatchedLimboAPI") {
    group = "limboapi"
    description = "构建修复后的 LimboAPI JAR（添加缺失的 mapping 文件）"

    inputs.file(baseJar)
    inputs.dir(mappingDir)
    outputs.file(patchedJar)

    doLast {
        val output = patchedJar.get()
        output.parentFile.mkdirs()

        // Copy the base JAR
        baseJar.copyTo(output, overwrite = true)

        // Add the missing mapping files
        val zaFile = java.util.zip.ZipFile(baseJar)
        val skipNames = setOf(
            "mapping/data_component_types.json",
            "mapping/data_component_types_mapping.json"
        )

        val mappingFiles = listOf(
            "data_component_types.json",
            "data_component_types_mapping.json"
        )
        val fos = java.io.FileOutputStream(output)
        val zos = java.util.zip.ZipOutputStream(fos)

        zaFile.entries().asSequence().forEach { entry ->
            if (entry.name.startsWith("META-INF/")) return@forEach
            if (entry.name in skipNames) return@forEach
            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
            zaFile.getInputStream(entry).transferTo(zos)
            zos.closeEntry()
        }

        mappingFiles.forEach { name ->
            val file = File(mappingDir, name)
            if (file.exists()) {
                zos.putNextEntry(java.util.zip.ZipEntry("mapping/$name"))
                file.inputStream().transferTo(zos)
                zos.closeEntry()
            }
        }

        zaFile.close()
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
