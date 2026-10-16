package com.sally.zlauncher.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Penyimpanan versi hasil impor.
 *
 * Struktur di filesDir:
 *   versions/<id>/base.apk, splits/*.apk, meta.json
 *   runtime/<id>/lib/arm64/*.so   (hasil ekstraksi library native)
 */
class VersionStore(private val context: Context) {

    private val root: File
        get() = File(context.filesDir, "versions").apply { mkdirs() }

    fun versionDir(id: String): File = File(root, id)

    fun runtimeDir(id: String): File = File(context.filesDir, "runtime/$id")

    fun list(): List<GameVersion> {
        val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { readMeta(it) }.sortedByDescending { it.createdAt }
    }

    fun apkFiles(id: String): List<File> {
        val dir = versionDir(id)
        val base = File(dir, BASE_APK)
        val splits = File(dir, SPLITS_DIR)
            .listFiles { f -> f.isFile && f.name.endsWith(".apk") }
            ?.sortedBy { it.name }
            ?: emptyList()
        return (listOf(base) + splits).filter { it.isFile }
    }

    fun writeMeta(id: String, displayName: String, versionName: String) {
        val json = JSONObject()
            .put("displayName", displayName)
            .put("versionName", versionName)
            .put("createdAt", System.currentTimeMillis())
        File(versionDir(id), META).writeText(json.toString())
    }

    fun delete(id: String) {
        versionDir(id).deleteRecursively()
        runtimeDir(id).deleteRecursively()
    }

    fun uniqueId(base: String): String {
        val clean = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        var id = clean
        var n = 2
        while (versionDir(id).exists()) {
            id = "${clean}_$n"
            n++
        }
        return id
    }

    private fun readMeta(dir: File): GameVersion? {
        val metaFile = File(dir, META)
        if (!metaFile.isFile || !File(dir, BASE_APK).isFile) return null
        return try {
            val json = JSONObject(metaFile.readText())
            GameVersion(
                id = dir.name,
                displayName = json.optString("displayName", dir.name),
                versionName = json.optString("versionName", "?"),
                isInstalled = false,
                sizeBytes = apkFiles(dir.name).sumOf { it.length() },
                createdAt = json.optLong("createdAt", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val META = "meta.json"
        const val BASE_APK = "base.apk"
        const val SPLITS_DIR = "splits"
    }
}
