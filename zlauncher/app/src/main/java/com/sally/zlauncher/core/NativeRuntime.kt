package com.sally.zlauncher.core

import android.content.Context
import com.sally.zlauncher.data.GameVersion
import com.sally.zlauncher.data.VersionStore
import java.io.File
import java.util.zip.ZipFile

/**
 * Tahap B (bagian awal): menyiapkan library native Minecraft.
 *
 * Yang sudah dikerjakan di sini:
 *  - menemukan semua APK sumber (base + split),
 *  - mengekstrak lib/arm64-v8a/*.so ke penyimpanan launcher (read-only, dengan stamp cache),
 *  - memastikan libminecraftpe.so ada.
 *
 * Yang BELUM ada: game host (Activity + kelas Java yang diharapkan libminecraftpe.so).
 * Karena itu [loadLibraries] sengaja tidak dipanggil dari UI.
 */
object NativeRuntime {

    private const val ABI_PREFIX = "lib/arm64-v8a/"
    private const val MAIN_LIB = "libminecraftpe.so"

    // Urutan muat yang umum; library yang tidak ada pada suatu versi dilewati.
    private val LOAD_ORDER = listOf(
        "libc++_shared.so",
        "libfmod.so",
        "libMediaDecoders_Android.so",
        "libMallocRedirect.so",
        "libHttpClient.Android.so",
        MAIN_LIB
    )

    data class Prepared(val libDir: File, val libs: List<String>)

    fun libDir(store: VersionStore, versionId: String): File =
        File(store.runtimeDir(versionId), "lib/arm64")

    fun prepare(context: Context, store: VersionStore, version: GameVersion): Prepared {
        val apks = sourceApks(context, store, version)
        if (apks.isEmpty()) throw IllegalStateException("APK sumber tidak ditemukan")
        LaunchLog.add("APK sumber: ${apks.size} file")

        val dir = libDir(store, version.id)
        val stampFile = File(dir.parentFile, "stamp.txt")
        val stamp = apks.joinToString("|") { "${it.name}:${it.length()}:${it.lastModified()}" }

        if (stampFile.isFile && stampFile.readText() == stamp && File(dir, MAIN_LIB).isFile) {
            LaunchLog.add("Cache library masih valid, ekstraksi dilewati")
        } else {
            dir.deleteRecursively()
            dir.mkdirs()
            val count = extract(apks, dir)
            LaunchLog.add("Mengekstrak $count library arm64-v8a")
            if (count == 0 && version.isInstalled) copyFromNativeDir(context, dir)
            stampFile.writeText(stamp)
        }

        val libs = dir.listFiles { f -> f.isFile && f.name.endsWith(".so") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        if (MAIN_LIB !in libs) {
            throw IllegalStateException("$MAIN_LIB tidak ditemukan. Pastikan APK arm64 yang diimpor.")
        }
        libs.forEach { LaunchLog.add("  • $it") }
        return Prepared(dir, libs)
    }

    /**
     * Memuat library ke proses ini. Jangan dipanggil sebelum game host tersedia:
     * JNI_OnLoad milik game mengharapkan kelas Java tertentu dan bisa menutup aplikasi.
     */
    @Suppress("unused")
    fun loadLibraries(prepared: Prepared) {
        for (name in LOAD_ORDER) {
            val file = File(prepared.libDir, name)
            if (!file.isFile) continue
            LaunchLog.add("System.load $name")
            System.load(file.absolutePath)
        }
    }

    @Suppress("DEPRECATION")
    private fun sourceApks(context: Context, store: VersionStore, version: GameVersion): List<File> {
        if (!version.isInstalled) return store.apkFiles(version.id)
        val info = context.packageManager.getApplicationInfo(McPackage.NAME, 0)
        val files = ArrayList<File>()
        files.add(File(info.sourceDir))
        info.splitSourceDirs?.forEach { files.add(File(it)) }
        return files.filter { it.isFile }
    }

    private fun extract(apks: List<File>, outDir: File): Int {
        var count = 0
        for (apk in apks) {
            ZipFile(apk).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    if (!entry.name.startsWith(ABI_PREFIX) || !entry.name.endsWith(".so")) continue
                    val out = File(outDir, entry.name.substringAfterLast('/'))
                    if (out.exists()) continue
                    zip.getInputStream(entry).use { src ->
                        out.outputStream().use { dst -> src.copyTo(dst) }
                    }
                    out.setReadOnly()
                    count++
                }
            }
        }
        return count
    }

    @Suppress("DEPRECATION")
    private fun copyFromNativeDir(context: Context, outDir: File) {
        val info = context.packageManager.getApplicationInfo(McPackage.NAME, 0)
        val nativeDir = info.nativeLibraryDir?.let { File(it) } ?: return
        nativeDir.listFiles { f -> f.isFile && f.name.endsWith(".so") }?.forEach { src ->
            val out = File(outDir, src.name)
            if (!out.exists()) {
                src.copyTo(out)
                out.setReadOnly()
            }
        }
    }
}
