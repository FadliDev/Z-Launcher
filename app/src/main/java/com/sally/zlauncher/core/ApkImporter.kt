package com.sally.zlauncher.core

import android.content.Context
import android.net.Uri
import com.sally.zlauncher.data.GameVersion
import com.sally.zlauncher.data.VersionStore
import java.io.File
import java.util.zip.ZipFile

class ImportException(message: String) : Exception(message)

/**
 * Mengimpor Minecraft Bedrock dari .apk (tunggal) atau .apks / .xapk (bundle berisi split).
 * Hasilnya disalin ke penyimpanan launcher, bukan dipasang ke sistem.
 */
class ApkImporter(
    private val context: Context,
    private val store: VersionStore
) {

    fun import(uri: Uri): GameVersion {
        val workDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
        workDir.mkdirs()
        var createdId: String? = null
        try {
            val source = File(workDir, "source.bin")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw ImportException("File tidak bisa dibaca")
            input.use { src -> source.outputStream().use { dst -> src.copyTo(dst) } }

            val (base, splits) = collect(source, workDir)

            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageArchiveInfo(base.absolutePath, 0)
                ?: throw ImportException("File bukan APK yang valid")
            if (info.packageName != McPackage.NAME) {
                throw ImportException("Bukan APK Minecraft Bedrock (paket: ${info.packageName})")
            }
            if (!hasArm64Libs(listOf(base) + splits)) {
                throw ImportException(
                    "Library arm64-v8a tidak ditemukan. Impor .apks yang memuat split arm64, atau APK arm64."
                )
            }

            val versionName = info.versionName ?: "unknown"
            val id = store.uniqueId("mc_$versionName")
            createdId = id
            val dir = store.versionDir(id)
            File(dir, VersionStore.SPLITS_DIR).mkdirs()
            move(base, File(dir, VersionStore.BASE_APK))
            splits.forEachIndexed { index, file ->
                move(file, File(dir, "${VersionStore.SPLITS_DIR}/split_$index.apk"))
            }
            store.writeMeta(id, "Minecraft $versionName", versionName)
            createdId = null
            return store.list().first { it.id == id }
        } catch (e: Exception) {
            createdId?.let { store.delete(it) }
            throw e
        } finally {
            workDir.deleteRecursively()
        }
    }

    /** Mengembalikan (base.apk, daftar split). File .apk tunggal dianggap base tanpa split. */
    private fun collect(source: File, workDir: File): Pair<File, List<File>> {
        val apkNames = try {
            ZipFile(source).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                    .map { it.name }
                    .toList()
            }
        } catch (e: Exception) {
            throw ImportException("File bukan arsip atau APK yang valid")
        }

        if (apkNames.isEmpty()) {
            val base = File(workDir, "base.apk")
            move(source, base)
            return base to emptyList()
        }

        // Bundle: ekstrak tiap .apk ke nama aman buatan sendiri (tanpa memakai path dari arsip).
        val extracted = ArrayList<Pair<String, File>>()
        ZipFile(source).use { zip ->
            apkNames.forEachIndexed { index, name ->
                val entry = zip.getEntry(name)
                val out = File(workDir, "part_$index.apk")
                zip.getInputStream(entry).use { src ->
                    out.outputStream().use { dst -> src.copyTo(dst) }
                }
                extracted.add(name.substringAfterLast('/') to out)
            }
        }

        val baseEntry = extracted.firstOrNull { it.first.equals("base.apk", ignoreCase = true) }
            ?: extracted.firstOrNull {
                val n = it.first.lowercase()
                !n.startsWith("split_") && !n.startsWith("config.")
            }
            ?: extracted.first()
        val splits = extracted.filter { it !== baseEntry }.map { it.second }
        return baseEntry.second to splits
    }

    private fun hasArm64Libs(apks: List<File>): Boolean {
        for (apk in apks) {
            try {
                ZipFile(apk).use { zip ->
                    val found = zip.entries().asSequence().any {
                        it.name.startsWith("lib/arm64-v8a/") && it.name.endsWith(".so")
                    }
                    if (found) return true
                }
            } catch (e: Exception) {
                // abaikan APK yang tidak bisa dibaca, lanjut ke berikutnya
            }
        }
        return false
    }

    private fun move(from: File, to: File) {
        to.parentFile?.mkdirs()
        if (!from.renameTo(to)) {
            from.copyTo(to, overwrite = true)
            from.delete()
        }
    }
}
