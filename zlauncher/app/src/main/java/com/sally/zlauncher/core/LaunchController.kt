package com.sally.zlauncher.core

import android.content.Context
import android.content.Intent
import com.sally.zlauncher.data.GameVersion
import com.sally.zlauncher.data.VersionStore

data class LaunchResult(
    val ok: Boolean,
    val message: String,
    /** Jika tidak null, Activity harus memulainya di main thread. */
    val intent: Intent? = null
)

/**
 * Menentukan cara meluncurkan sebuah versi.
 *  - Game terpasang -> buka aplikasi Minecraft resmi.
 *  - Versi hasil impor -> siapkan library native (game host menyusul di Tahap B).
 */
class LaunchController(
    private val context: Context,
    private val store: VersionStore
) {

    /** Boleh dipanggil dari thread latar. */
    fun launch(version: GameVersion): LaunchResult {
        LaunchLog.clear()
        LaunchLog.add("Memulai ${version.displayName} (${version.versionName})")
        return try {
            requireLicense()
            if (version.isInstalled) launchInstalled() else prepareImported(version)
        } catch (e: Exception) {
            LaunchLog.add("GAGAL: ${e.message ?: e.javaClass.simpleName}")
            LaunchResult(false, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun requireLicense() {
        when (PlayStoreValidator.state(context)) {
            McInstallState.PLAY_STORE -> LaunchLog.add("Lisensi: terpasang dari Google Play")
            McInstallState.NOT_INSTALLED -> throw IllegalStateException("Minecraft belum terpasang dari Google Play")
            McInstallState.OTHER_SOURCE -> throw IllegalStateException("Lisensi tidak bisa diverifikasi (bukan dari Google Play)")
        }
    }

    private fun launchInstalled(): LaunchResult {
        val intent = context.packageManager.getLaunchIntentForPackage(McPackage.NAME)
            ?: throw IllegalStateException("Aplikasi Minecraft tidak bisa dibuka")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        LaunchLog.add("Membuka Minecraft resmi")
        return LaunchResult(true, "Membuka Minecraft…", intent)
    }

    private fun prepareImported(version: GameVersion): LaunchResult {
        val prepared = NativeRuntime.prepare(context, store, version)
        val message = "Library native siap (${prepared.libs.size} file).\n" +
            "Game host belum diimplementasikan (Tahap B), jadi game belum bisa dijalankan dari versi impor. " +
            "Lihat README."
        LaunchLog.add("Selesai menyiapkan library")
        return LaunchResult(true, message)
    }
}
